package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.wellsql.generated.MediaModelTable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.FadeInNetworkImageView;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter for the media gallery grid.
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.GridViewHolder> {
    private MediaGridAdapterCallback mCallback;
    private boolean mHasRetrievedAll;
    private boolean mIsRefreshing;
    private int mGridItemWidth;
    private final Map<String, List<BitmapReadyCallback>> mFilePathToCallbackMap;
    private final Handler mHandler;
    private final int mImageSize;
    private final LayoutInflater mInflater;
    private ImageLoader mImageLoader;
    private Context mContext;
    private SiteModel mSite;
    private Cursor mCursor;

    // Must be an ArrayList (order is important for galleries)
    private ArrayList<Integer> mSelectedItems;

    public interface MediaGridAdapterCallback {
        void fetchMoreData();
        void onRetryUpload(int localMediaId);
        boolean isInMultiSelect();
    }

    interface BitmapReadyCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    public MediaGridAdapter(Context context, SiteModel site, ImageLoader imageLoader) {
        super();
        mContext = context;
        mSite = site;
        mSelectedItems = new ArrayList<>();
        mImageSize = context.getResources().getDimensionPixelSize(R.dimen.media_grid_image_size);
        mInflater = LayoutInflater.from(context);
        mFilePathToCallbackMap = new HashMap<>();
        mHandler = new Handler();
        setImageLoader(imageLoader);
    }

    void setImageLoader(ImageLoader imageLoader) {
        if (imageLoader != null) {
            mImageLoader = imageLoader;
        } else {
            mImageLoader = WordPress.sImageLoader;
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor cursor) {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.media_grid_item, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GridViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        final int localMediaId = mCursor.getInt(mCursor.getColumnIndex(MediaModelTable.ID));

        String state = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.UPLOAD_STATE));
        boolean isLocalFile = MediaUtils.isLocalFile(state);

        // file name
        String fileName = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.FILE_NAME));
        if (holder.filenameView != null) {
            holder.filenameView.setText(fileName);
        }

        // title of media
        String title = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.TITLE));
        if (TextUtils.isEmpty(title)) {
            title = fileName;
        }
        holder.titleView.setText(title);

        // upload date
        if (holder.uploadDateView != null) {
            String date = MediaUtils.getDate(mCursor.getLong(mCursor.getColumnIndex(MediaModelTable.UPLOAD_DATE)));
            holder.uploadDateView.setText(date);
        }

        // load image
        if (isLocalFile) {
            loadLocalImage(mCursor, holder.imageView);
        } else {
            String thumbUrl = WordPressMediaUtils.getNetworkThumbnailUrl(mCursor, mSite, mGridItemWidth);
            WordPressMediaUtils.loadNetworkImage(thumbUrl, holder.imageView, mImageLoader);
        }

        // get the file extension from the fileURL
        String mimeType = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.MIME_TYPE));
        String fileExtension = MediaUtils.getExtensionForMimeType(mimeType);
        fileExtension = fileExtension.toUpperCase();
        // file type
        if  (DisplayUtils.isXLarge(mContext) && !TextUtils.isEmpty(fileExtension)) {
            holder.fileTypeView.setText(String.format(mContext.getString(R.string.media_file_type), fileExtension));
        } else {
            holder.fileTypeView.setText(fileExtension);
        }

        // dimensions
        String filePath = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.FILE_PATH));
        if (holder.dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = mCursor.getInt(mCursor.getColumnIndex(MediaModelTable.WIDTH));
                int height = mCursor.getInt(mCursor.getColumnIndex(MediaModelTable.HEIGHT));

                if (width > 0 && height > 0) {
                    String dimensions = width + "x" + height;
                    holder.dimensionView.setText(dimensions);
                    holder.dimensionView.setVisibility(View.VISIBLE);
                }
            } else {
                holder.dimensionView.setVisibility(View.GONE);
            }
        }

        holder.frameLayout.setTag(localMediaId);
        holder.frameLayout.setChecked(mSelectedItems.contains(localMediaId));

        // show upload state
        if (holder.stateTextView != null) {
            if (state != null && state.length() > 0) {
                holder.stateTextView.setVisibility(View.VISIBLE);
                holder.stateTextView.setText(state);

                // hide the progressbar only if the state is Uploaded or failed
                if (state.equalsIgnoreCase(MediaUploadState.UPLOADED.name()) ||
                        state.equalsIgnoreCase(MediaUploadState.FAILED.name())) {
                    holder.progressUpload.setVisibility(View.GONE);
                } else {
                    holder.progressUpload.setVisibility(View.VISIBLE);
                }

                // Hide the state text only if the it's Uploaded
                if (state.equalsIgnoreCase(MediaUploadState.UPLOADED.name())) {
                    holder.stateTextView.setVisibility(View.GONE);
                }

                // add onclick to retry failed uploads
                if (state.equalsIgnoreCase(MediaUploadState.FAILED.name())) {
                    holder.stateTextView.setVisibility(View.VISIBLE);
                    holder.stateTextView.setText(mContext.getString(R.string.retry));
                    holder.stateTextView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!inMultiSelect()) {
                                ((TextView) v).setText(R.string.upload_queued);
                                v.setOnClickListener(null);
                                mCallback.onRetryUpload(localMediaId);
                            }
                        }
                    });
                }

            } else {
                holder.progressUpload.setVisibility(View.GONE);
                holder.stateTextView.setVisibility(View.GONE);
            }
        }

        // if we are near the end, make a call to fetch more
        if (position == getItemCount() - 1 && !mHasRetrievedAll) {
            if (mCallback != null) {
                mCallback.fetchMoreData();
            }
        }
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private final TextView filenameView;
        private final TextView titleView;
        private final TextView uploadDateView;
        private final FadeInNetworkImageView imageView;
        private final TextView fileTypeView;
        private final TextView dimensionView;
        private final CheckableFrameLayout frameLayout;

        private final TextView stateTextView;
        private final ProgressBar progressUpload;

        public GridViewHolder(View view) {
            super(view);

            filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
            titleView = (TextView) view.findViewById(R.id.media_grid_item_name);
            uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
            imageView = (FadeInNetworkImageView) view.findViewById(R.id.media_grid_item_image);
            fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
            dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
            frameLayout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);

            stateTextView = (TextView) view.findViewById(R.id.media_grid_item_upload_state);
            progressUpload = (ProgressBar) view.findViewById(R.id.media_grid_item_upload_progress);
        }
    }

    private boolean inMultiSelect() {
        return mCallback.isInMultiSelect();
    }

    private synchronized void loadLocalImage(Cursor cursor, final FadeInNetworkImageView imageView) {
        final String filePath = cursor.getString(cursor.getColumnIndex(MediaModelTable.FILE_PATH));
        imageView.setTag(filePath);

        Bitmap bitmap = WordPress.getBitmapCache().get(filePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageBitmap(null);

            boolean shouldFetch = false;

            List<BitmapReadyCallback> list;
            if (mFilePathToCallbackMap.containsKey(filePath)) {
                list = mFilePathToCallbackMap.get(filePath);
            } else {
                list = new ArrayList<>();
                shouldFetch = true;
                mFilePathToCallbackMap.put(filePath, list);
            }
            list.add(new BitmapReadyCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    if (imageView.getTag() instanceof String && imageView.getTag().equals(filePath)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            });

            if (shouldFetch) {
                fetchBitmap(filePath);
            }
        }
    }

    private void fetchBitmap(final String filePath) {
        BitmapWorkerTask task = new BitmapWorkerTask(null, mImageSize, mImageSize, new BitmapWorkerCallback() {
            @Override
            public void onBitmapReady(final String path, ImageView imageView, final Bitmap bitmap) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        List<BitmapReadyCallback> callbacks = mFilePathToCallbackMap.get(path);
                        for (BitmapReadyCallback callback : callbacks) {
                            callback.onBitmapReady(bitmap);
                        }

                        WordPress.getBitmapCache().put(path, bitmap);
                        callbacks.clear();
                        mFilePathToCallbackMap.remove(path);
                    }
                });
            }
        });
        task.execute(filePath);
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    /** Return the number of columns in the media grid **/
    public static int getColumnCount(Context context) {
        return context.getResources().getInteger(R.integer.media_grid_num_columns);
    }

    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }

    public void setHasRetrievedAll(boolean b) {
        mHasRetrievedAll = b;
    }

    public void setRefreshing(boolean refreshing) {
        mIsRefreshing = refreshing;
        //notifyDataSetChanged();
    }

    private void setGridItemWidth() {
        int maxWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int columnCount = getColumnCount(mContext);
        if (columnCount > 0) {
            int dp8 = DisplayUtils.dpToPx(mContext, 8);
            int padding = (columnCount + 1) * dp8;
            mGridItemWidth = (maxWidth - padding) / columnCount;
        }
    }

    public void clearSelection() {
        mSelectedItems.clear();
    }

    public boolean isItemSelected(int localMediaId) {
        return mSelectedItems.contains(localMediaId);
    }

    public void setItemSelectedByPosition(int position, boolean selected) {
        if (mCursor == null) {
            return;
        }
        mCursor.moveToPosition(position);
        int columnIndex = mCursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex != -1) {
            int localMediaId = mCursor.getInt(columnIndex);
            setItemSelectedByLocalId(localMediaId, selected);
        }
    }

    public void setItemSelectedByLocalId(int localMediaId, boolean selected) {
        if (selected) {
            mSelectedItems.add(localMediaId);
        } else {
            mSelectedItems.remove(Integer.valueOf(localMediaId));
        }
        notifyDataSetChanged();
    }

    public void toggleItemSelected(int position) {
        if (mCursor == null) {
            return;
        }
        mCursor.moveToPosition(position);
        int columnIndex = mCursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex != -1) {
            int localMediaId = mCursor.getInt(columnIndex);
            if (mSelectedItems.contains(localMediaId)) {
                mSelectedItems.remove(Integer.valueOf(localMediaId));
            } else {
                mSelectedItems.add(localMediaId);
            }
            notifyDataSetChanged();
        }
    }

    public void setSelectedItems(ArrayList<Integer> selectedItems) {
        mSelectedItems = selectedItems;
        notifyDataSetChanged();
    }
}
