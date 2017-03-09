package org.wordpress.android.ui.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.wellsql.generated.MediaModelTable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter for the media gallery listViews.
 */
public class MediaGridAdapter extends CursorAdapter {
    private MediaGridAdapterCallback mCallback;
    private boolean mHasRetrievedAll;
    private boolean mIsRefreshing;
    private int mCursorDataCount;
    private int mGridItemWidth;
    private final Map<String, List<BitmapReadyCallback>> mFilePathToCallbackMap;
    private final Handler mHandler;
    private final int mLocalImageWidth;
    private final LayoutInflater mInflater;
    private ImageLoader mImageLoader;
    private Context mContext;
    private SiteModel mSite;

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

    private enum ViewTypes {
        LOCAL, NETWORK, PROGRESS, SPACER
    }

    public MediaGridAdapter(Context context, SiteModel site, Cursor c, int flags, ImageLoader imageLoader) {
        super(context, c, flags);
        mContext = context;
        mSite = site;
        mSelectedItems = new ArrayList<>();
        mLocalImageWidth = context.getResources().getDimensionPixelSize(R.dimen.media_grid_local_image_width);
        mInflater = LayoutInflater.from(context);
        mFilePathToCallbackMap = new HashMap<>();
        mHandler = new Handler();
        setImageLoader(imageLoader);
    }

    void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    private static class GridViewHolder {
        private final TextView filenameView;
        private final TextView titleView;
        private final TextView uploadDateView;
        private final ImageView imageView;
        private final TextView fileTypeView;
        private final TextView dimensionView;
        private final CheckableFrameLayout frameLayout;

        private final TextView stateTextView;
        private final ProgressBar progressUpload;

        GridViewHolder(View view) {
            filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
            titleView = (TextView) view.findViewById(R.id.media_grid_item_name);
            uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
            imageView = (ImageView) view.findViewById(R.id.media_grid_item_image);
            fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);
            dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
            frameLayout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);

            stateTextView = (TextView) view.findViewById(R.id.media_grid_item_upload_state);
            progressUpload = (ProgressBar) view.findViewById(R.id.media_grid_item_upload_progress);
        }
    }

	@SuppressLint("DefaultLocale")
	@Override
    public void bindView(final View view, Context context, Cursor cursor) {
        int itemViewType = getItemViewType(cursor.getPosition());

        if (itemViewType == ViewTypes.PROGRESS.ordinal()) {
            if (mIsRefreshing) {
                int height = mContext.getResources().getDimensionPixelSize(R.dimen.media_grid_progress_height);
                view.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, height));
                view.setVisibility(View.VISIBLE);
            } else {
                view.setLayoutParams(new GridView.LayoutParams(0, 0));
                view.setVisibility(View.GONE);
            }
            return;
        } else if (itemViewType == ViewTypes.SPACER.ordinal()) {
            CheckableFrameLayout frameLayout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);
            updateGridWidth(context, frameLayout);
            view.setVisibility(View.INVISIBLE);
            return;
        }

        final GridViewHolder holder;
        if (view.getTag() instanceof GridViewHolder) {
            holder = (GridViewHolder) view.getTag();
        } else {
            holder = new GridViewHolder(view);
            view.setTag(holder);
        }

        final int localMediaId = cursor.getInt(cursor.getColumnIndex(MediaModelTable.ID));

        String state = cursor.getString(cursor.getColumnIndex(MediaModelTable.UPLOAD_STATE));
        boolean isLocalFile = MediaUtils.isLocalFile(state);

        // file name
        String fileName = cursor.getString(cursor.getColumnIndex(MediaModelTable.FILE_NAME));
        if (holder.filenameView != null) {
            holder.filenameView.setText(fileName);
        }

        // title of media
        String title = cursor.getString(cursor.getColumnIndex(MediaModelTable.TITLE));
        if (title == null || title.equals(""))
            title = fileName;
        holder.titleView.setText(title);

        // upload date
        if (holder.uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex(MediaModelTable.UPLOAD_DATE)));
            holder.uploadDateView.setText(date);
        }

        // load image
        if (isLocalFile) {
            loadLocalImage(cursor, holder.imageView);
        } else {
            String thumbUrl = WordPressMediaUtils.getNetworkThumbnailUrl(cursor, mSite, mGridItemWidth);
            WordPressMediaUtils.loadNetworkImage(thumbUrl, (NetworkImageView) holder.imageView, mImageLoader);
        }

        // get the file extension from the fileURL
        String mimeType = cursor.getString(cursor.getColumnIndex(MediaModelTable.MIME_TYPE));
        String fileExtension = MediaUtils.getExtensionForMimeType(mimeType);
        fileExtension = fileExtension.toUpperCase();
        // file type
        if  (DisplayUtils.isXLarge(context) && !TextUtils.isEmpty(fileExtension)) {
            holder.fileTypeView.setText(String.format(context.getString(R.string.media_file_type), fileExtension));
        } else {
            holder.fileTypeView.setText(fileExtension);
        }

        // dimensions
        String filePath = cursor.getString(cursor.getColumnIndex(MediaModelTable.FILE_PATH));
        TextView dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
        if (dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = cursor.getInt(cursor.getColumnIndex(MediaModelTable.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(MediaModelTable.HEIGHT));

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

        // resizing layout to fit nicely into grid view
        updateGridWidth(context, holder.frameLayout);

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
                    holder.stateTextView.setText(context.getString(R.string.retry));
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
        int position = cursor.getPosition();
        if (position == mCursorDataCount - 1 && !mHasRetrievedAll) {
            if (mCallback != null) {
                mCallback.fetchMoreData();
            }
        }
    }

    private boolean inMultiSelect() {
        return mCallback.isInMultiSelect();
    }

    private synchronized void loadLocalImage(Cursor cursor, final ImageView imageView) {
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
        BitmapWorkerTask task = new BitmapWorkerTask(null, mLocalImageWidth, mLocalImageWidth, new BitmapWorkerCallback() {
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
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        int itemViewType = getItemViewType(cursor.getPosition());

        // spacer and progress spinner views
        if (itemViewType == ViewTypes.PROGRESS.ordinal()) {
            return mInflater.inflate(R.layout.media_grid_progress, root, false);
        } else if (itemViewType == ViewTypes.SPACER.ordinal()) {
            return mInflater.inflate(R.layout.media_grid_item, root, false);
        }

        View view =  mInflater.inflate(R.layout.media_grid_item, root, false);
        ViewStub imageStub = (ViewStub) view.findViewById(R.id.media_grid_image_stub);

        // We need to use ViewStubs to inflate the image to either:
        // - a regular ImageView (for local images)
        // - a FadeInNetworkImageView (for network images)
        // This is because the NetworkImageView can't load local images.
        // The other option would be to inflate multiple layouts, but that would lead
        // to extra near-duplicate xml files that would need to be maintained.
        if (itemViewType == ViewTypes.LOCAL.ordinal()) {
            imageStub.setLayoutResource(R.layout.media_grid_image_local);
        } else {
            imageStub.setLayoutResource(R.layout.media_grid_image_network);
        }

        imageStub.inflate();

        view.setTag(new GridViewHolder(view));

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return ViewTypes.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);

        // spacer / progress cells
        int _id = cursor.getInt(cursor.getColumnIndex("_id"));
        if (_id < 0) {
            if (_id == Integer.MIN_VALUE)
                return ViewTypes.PROGRESS.ordinal();
            else
                return ViewTypes.SPACER.ordinal();
        }

        // regular cells
        String state = cursor.getString(cursor.getColumnIndex(MediaModelTable.UPLOAD_STATE));
        if (MediaUtils.isLocalFile(state))
            return ViewTypes.LOCAL.ordinal();
        else
            return ViewTypes.NETWORK.ordinal();
    }

    /** Updates the width of a cell to max out the space available, for phones **/
    private void updateGridWidth(Context context, View view) {
        setGridItemWidth();
        int columnCount = getColumnCount(context);

        if (columnCount > 1) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mGridItemWidth, mGridItemWidth);
            view.setLayoutParams(params);
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == null) {
            mCursorDataCount = 0;
            return super.swapCursor(null);
        }

        mCursorDataCount = newCursor.getCount();

        // to mimic the infinite the notification's infinite scroll ui
        // (with a progress spinner on the bottom of the list), we'll need to add
        // extra cells in the gridview:
        // - spacer cells as fillers to place the progress spinner on the first cell (_id < 0)
        // - progress spinner cell (_id = Integer.MIN_VALUE)

        // use a matrix cursor to create the extra rows
        MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id" });

        // add spacer cells
        int columnCount = getColumnCount(mContext);
        int remainder = newCursor.getCount() % columnCount;
        if (remainder > 0) {
            int spaceCount = columnCount - remainder;
            for (int i = 0; i < spaceCount; i++ ) {
                int id = i - spaceCount;
                matrixCursor.addRow(new Object[] {id + ""});
            }
        }

        // add progress spinner cell
        matrixCursor.addRow(new Object[] { Integer.MIN_VALUE });

        // use a merge cursor to place merge the extra rows at the bottom of the newly swapped cursor
        MergeCursor mergeCursor = new MergeCursor(new Cursor[] { newCursor, matrixCursor });
        return super.swapCursor(mergeCursor);
    }

    /** Return the number of columns in the media grid **/
    private int getColumnCount(Context context) {
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
        notifyDataSetChanged();
    }

    public int getDataCount() {
        return mCursorDataCount;
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
        Cursor cursor = (Cursor) getItem(position);
        if (cursor == null) {
            return;
        }
        int columnIndex = cursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex != -1) {
            int localMediaId = cursor.getInt(columnIndex);
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
        Cursor cursor = (Cursor) getItem(position);
        int columnIndex = cursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex != -1) {
            int localMediaId = cursor.getInt(columnIndex);
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
