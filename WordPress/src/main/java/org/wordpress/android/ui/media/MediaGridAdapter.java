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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
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
    // Must be an ArrayList (order is important for galleries)
    private ArrayList<String> mSelectedItems;

    public interface MediaGridAdapterCallback {
        public void fetchMoreData(int offset);
        public void onRetryUpload(String mediaId);
        public boolean isInMultiSelect();
    }

    interface BitmapReadyCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    private static enum ViewTypes {
        LOCAL, NETWORK, PROGRESS, SPACER
    }

    public MediaGridAdapter(Context context, Cursor c, int flags, ImageLoader imageLoader) {
        super(context, c, flags);
        mContext = context;
        mSelectedItems = new ArrayList<String>();
        mLocalImageWidth = context.getResources().getDimensionPixelSize(R.dimen.media_grid_local_image_width);
        mInflater = LayoutInflater.from(context);
        mFilePathToCallbackMap = new HashMap<String, List<BitmapReadyCallback>>();
        mHandler = new Handler();
        setImageLoader(imageLoader);
    }

    void setImageLoader(ImageLoader imageLoader) {
        if (imageLoader != null) {
            mImageLoader = imageLoader;
        } else {
            mImageLoader = WordPress.imageLoader;
        }
    }

    public ArrayList<String> getSelectedItems() {
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
        private final RelativeLayout uploadStateView;

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
            uploadStateView = (RelativeLayout) view.findViewById(R.id.media_grid_item_upload_state_container);
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

        final String mediaId = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID));

        String state = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_UPLOAD_STATE));
        boolean isLocalFile = MediaUtils.isLocalFile(state);

        // file name
        String fileName = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME));
        if (holder.filenameView != null) {
            holder.filenameView.setText(fileName);
        }

        // title of media
        String title = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_TITLE));
        if (title == null || title.equals(""))
            title = fileName;
        holder.titleView.setText(title);

        // upload date
        if (holder.uploadDateView != null) {
            String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DATE_CREATED_GMT)));
            holder.uploadDateView.setText(date);
        }

        // load image
        if (isLocalFile) {
            loadLocalImage(cursor, holder.imageView);
        } else {
            String thumbUrl = WordPressMediaUtils.getNetworkThumbnailUrl(cursor, mGridItemWidth);
            WordPressMediaUtils.loadNetworkImage(thumbUrl, (NetworkImageView) holder.imageView, mImageLoader);
        }

        // get the file extension from the fileURL
        String mimeType = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MIME_TYPE));
        String fileExtension = MediaUtils.getExtensionForMimeType(mimeType);
        fileExtension = fileExtension.toUpperCase();
        // file type
        if  (DisplayUtils.isXLarge(context) && !TextUtils.isEmpty(fileExtension)) {
            holder.fileTypeView.setText(String.format(context.getString(R.string.media_file_type), fileExtension));
        } else {
            holder.fileTypeView.setText(fileExtension);
        }

        // dimensions
        String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL));
        TextView dimensionView = (TextView) view.findViewById(R.id.media_grid_item_dimension);
        if (dimensionView != null) {
            if( MediaUtils.isValidImage(filePath)) {
                int width = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_HEIGHT));

                if (width > 0 && height > 0) {
                    String dimensions = width + "x" + height;
                    holder.dimensionView.setText(dimensions);
                    holder.dimensionView.setVisibility(View.VISIBLE);
                }
            } else {
                holder.dimensionView.setVisibility(View.GONE);
            }
        }

        holder.frameLayout.setTag(mediaId);
        holder.frameLayout.setChecked(mSelectedItems.contains(mediaId));

        // resizing layout to fit nicely into grid view
        updateGridWidth(context, holder.frameLayout);

        // show upload state
        if (holder.stateTextView != null) {
            if (state != null && state.length() > 0) {
                // show the progressbar only when the state is uploading
                if (state.equals("uploading")) {
                    holder.progressUpload.setVisibility(View.VISIBLE);
                } else {
                    holder.progressUpload.setVisibility(View.GONE);
                    if (state.equals("uploaded")) {
                        holder.stateTextView.setVisibility(View.GONE);
                    }
                }

                // add onclick to retry failed uploads
                if (state.equals("failed")) {
                    state = "retry";
                    holder.stateTextView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!inMultiSelect()) {
                                ((TextView) v).setText(R.string.upload_queued);
                                v.setOnClickListener(null);
                                mCallback.onRetryUpload(mediaId);
                            }
                        }

                    });
                }

                holder.stateTextView.setText(state);
                holder.uploadStateView.setVisibility(View.VISIBLE);
            } else {
                holder.uploadStateView.setVisibility(View.GONE);
            }
        }

        // if we are near the end, make a call to fetch more
        int position = cursor.getPosition();
        if (position == mCursorDataCount - 1 && !mHasRetrievedAll) {
            if (mCallback != null) {
                mCallback.fetchMoreData(mCursorDataCount);
            }
        }
    }

    private boolean inMultiSelect() {
        return mCallback.isInMultiSelect();
    }

    private synchronized void loadLocalImage(Cursor cursor, final ImageView imageView) {
        final String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH));

        if (MediaUtils.isValidImage(filePath)) {
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
                    list = new ArrayList<MediaGridAdapter.BitmapReadyCallback>();
                    shouldFetch = true;
                    mFilePathToCallbackMap.put(filePath, list);
                }
                list.add(new BitmapReadyCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        if (imageView.getTag() instanceof String && imageView.getTag().equals(filePath))
                            imageView.setImageBitmap(bitmap);
                    }
                });


                if (shouldFetch) {
                    fetchBitmap(filePath);
                }
            }
        } else {
            // if not image, for now show no image.
            imageView.setImageBitmap(null);
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
        String state = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_UPLOAD_STATE));
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
            return super.swapCursor(newCursor);
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

    public boolean isItemSelected(String mediaId) {
        return mSelectedItems.contains(mediaId);
    }

    public void setItemSelected(int position, boolean selected) {
        Cursor cursor = (Cursor) getItem(position);
        if (cursor == null) {
            return;
        }
        int columnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID);
        if (columnIndex != -1) {
            String mediaId = cursor.getString(columnIndex);
            setItemSelected(mediaId, selected);
        }
    }

    public void setItemSelected(String mediaId, boolean selected) {
        if (selected) {
            mSelectedItems.add(mediaId);
        } else {
            mSelectedItems.remove(mediaId);
        }
        notifyDataSetChanged();
    }

    public void toggleItemSelected(int position) {
        Cursor cursor = (Cursor) getItem(position);
        int columnIndex = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID);
        if (columnIndex != -1) {
            String mediaId = cursor.getString(columnIndex);
            if (mSelectedItems.contains(mediaId)) {
                mSelectedItems.remove(mediaId);
            } else {
                mSelectedItems.add(mediaId);
            }
            notifyDataSetChanged();
        }
    }

    public void setSelectedItems(ArrayList<String> selectedItems) {
        mSelectedItems = selectedItems;
        notifyDataSetChanged();
    }
}
