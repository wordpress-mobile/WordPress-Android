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

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.wellsql.generated.MediaModelTable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.FadeInNetworkImageView;
import org.wordpress.android.util.AniUtils;
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

    private boolean mAllowMultiselect;
    private boolean mInMultiSelect;

    private final Map<String, List<BitmapReadyCallback>> mFilePathToCallbackMap;
    private final Handler mHandler;
    private final LayoutInflater mInflater;

    private ImageLoader mImageLoader;
    private final Context mContext;
    private final SiteModel mSite;
    private Cursor mCursor;

    private final int mThumbWidth;
    private final int mThumbHeight;

    // Must be an ArrayList (order is important for galleries)
    private ArrayList<Integer> mSelectedItems;

    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .85f;

    public interface MediaGridAdapterCallback {
        void onAdapterFetchMoreData();
        void onAdapterRetryUpload(int localMediaId);
        void onAdapterItemSelected(int position);
        void onAdapterSelectionCountChanged(int count);
    }

    interface BitmapReadyCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    private static final int INVALID_POSITION = -1;

    public MediaGridAdapter(Context context, SiteModel site, ImageLoader imageLoader) {
        super();
        setHasStableIds(true);

        mContext = context;
        mSite = site;
        mSelectedItems = new ArrayList<>();
        mInflater = LayoutInflater.from(context);
        mFilePathToCallbackMap = new HashMap<>();
        mHandler = new Handler();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        mThumbWidth = displayWidth / getColumnCount(mContext);
        mThumbHeight = (int) (mThumbWidth * 0.75f);

        setImageLoader(imageLoader);
    }

    @Override
    public long getItemId(int position) {
        return getLocalMediaIdAtPosition(position);
    }

    private void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public void setCursor(Cursor cursor) {
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
        if (!isValidPosition(position)) {
            return;
        }

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
        String filePath = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.FILE_PATH));
        String mimeType = mCursor.getString(mCursor.getColumnIndex(MediaModelTable.MIME_TYPE));
        if (mimeType.contains("image")) {
            if (isLocalFile) {
                loadLocalImage(filePath, holder.imageView);
            } else {
                String thumbUrl = WordPressMediaUtils.getNetworkThumbnailUrl(mCursor, mSite, mThumbWidth);
                WordPressMediaUtils.loadNetworkImage(thumbUrl, holder.imageView, mImageLoader);
            }
        } else {
            holder.imageView.setImageDrawable(null);
        }

        // get the file extension from the fileURL
        String fileExtension = MediaUtils.getExtensionForMimeType(mimeType);
        fileExtension = fileExtension.toUpperCase();
        // file type
        if (DisplayUtils.isXLarge(mContext) && !TextUtils.isEmpty(fileExtension)) {
            holder.fileTypeView.setText(String.format(mContext.getString(R.string.media_file_type), fileExtension));
        } else {
            holder.fileTypeView.setText(fileExtension);
        }

        boolean isSelected = isItemSelected(localMediaId);
        holder.selectionCountTextView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        if (isSelected) {
            int count = mSelectedItems.indexOf(localMediaId) + 1;
            holder.selectionCountTextView.setText(Integer.toString(count));
        }

        // make sure the thumbnail scale reflects its selection state
        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.imageView.getScaleX() != scale) {
            holder.imageView.setScaleX(scale);
            holder.imageView.setScaleY(scale);
        }

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
                            if (!isInMultiSelect()) {
                                ((TextView) v).setText(R.string.upload_queued);
                                v.setOnClickListener(null);
                                if (mCallback != null) {
                                    mCallback.onAdapterRetryUpload(localMediaId);
                                }
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
                mCallback.onAdapterFetchMoreData();
            }
        }
    }

    @Override
    public void onViewRecycled(GridViewHolder holder) {
        super.onViewRecycled(holder);

        // cancel image fetch requests if the view has been moved to recycler.
        if (holder.imageView != null) {
            String tag = (String) holder.imageView.getTag();
            if (tag != null && tag.startsWith("http")) {
                // need a listener to cancel request, even if the listener does nothing
                ImageLoader.ImageContainer container = mImageLoader.get(tag, new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { }

                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) { }

                });
                container.cancelRequest();
            }
        }
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private final TextView filenameView;
        private final TextView titleView;
        private final TextView uploadDateView;
        private final FadeInNetworkImageView imageView;
        private final TextView fileTypeView;
        private final TextView selectionCountTextView;
        private final TextView stateTextView;
        private final ProgressBar progressUpload;

        public GridViewHolder(View view) {
            super(view);

            filenameView = (TextView) view.findViewById(R.id.media_grid_item_filename);
            titleView = (TextView) view.findViewById(R.id.media_grid_item_name);
            uploadDateView = (TextView) view.findViewById(R.id.media_grid_item_upload_date);
            imageView = (FadeInNetworkImageView) view.findViewById(R.id.media_grid_item_image);
            fileTypeView = (TextView) view.findViewById(R.id.media_grid_item_filetype);

            selectionCountTextView = (TextView) view.findViewById(R.id.text_selection_count);
            stateTextView = (TextView) view.findViewById(R.id.media_grid_item_upload_state);
            progressUpload = (ProgressBar) view.findViewById(R.id.media_grid_item_upload_progress);

            imageView.getLayoutParams().width = mThumbWidth;
            imageView.getLayoutParams().height = mThumbHeight;

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isInMultiSelect()) {
                        toggleItemSelected(GridViewHolder.this, position);
                    } else if (mCallback != null) {
                        mCallback.onAdapterItemSelected(position);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (isInMultiSelect()) {
                        toggleItemSelected(GridViewHolder.this, position);
                    } else if (mAllowMultiselect) {
                        setInMultiSelect(true);
                        setItemSelectedByPosition(GridViewHolder.this, position, true);
                    }
                    return true;
                }
            });
        }
    }

    public void setAllowMultiselect(boolean allow) {
        mAllowMultiselect = allow;
    }

    public boolean isInMultiSelect() {
        return mInMultiSelect;
    }

    public void setInMultiSelect(boolean value) {
        if (mInMultiSelect != value) {
            mInMultiSelect = value;
            clearSelection();
        }
    }
    private boolean isValidPosition(int position) {
        return position >= 0 && position < getItemCount();
    }
    public int getLocalMediaIdAtPosition(int position) {
        if (isValidPosition(position)) {
            mCursor.moveToPosition(position);
            return mCursor.getInt(mCursor.getColumnIndex(MediaModelTable.ID));
        }
        return INVALID_POSITION;
    }

    private synchronized void loadLocalImage(final String filePath, final FadeInNetworkImageView imageView) {
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
        BitmapWorkerTask task = new BitmapWorkerTask(null, mThumbWidth, mThumbHeight, new BitmapWorkerCallback() {
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

    public void clearSelection() {
        if (mSelectedItems.size() > 0) {
            mSelectedItems.clear();
            notifyDataSetChanged();
        }
    }

    public boolean isItemSelected(int localMediaId) {
        return mSelectedItems.contains(localMediaId);
    }

    public void setItemSelectedByLocalId(int localMediaId, boolean selected) {
        if (selected) {
            mSelectedItems.add(localMediaId);
        } else {
            mSelectedItems.remove(Integer.valueOf(localMediaId));
        }
        if (mCallback != null) {
            mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
        }
        notifyDataSetChanged();
    }

    private void setItemSelectedByPosition(GridViewHolder holder, int position, boolean selected) {
        if (mCursor == null || !isValidPosition(position)) {
            return;
        }

        mCursor.moveToPosition(position);
        int columnIndex = mCursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex == -1) {
            return;
        }

        int localMediaId = mCursor.getInt(columnIndex);
        if (selected) {
            mSelectedItems.add(localMediaId);
        } else {
            mSelectedItems.remove(Integer.valueOf(localMediaId));
        }

        // show and animate the count
        if (selected) {
            holder.selectionCountTextView.setText(Integer.toString(mSelectedItems.indexOf(localMediaId)));
        }
        AniUtils.startAnimation(holder.selectionCountTextView,
                selected ? R.anim.cab_select : R.anim.cab_deselect);
        holder.selectionCountTextView.setVisibility(selected ? View.VISIBLE : View.GONE);

        // scale the thumbnail
        if (selected) {
            AniUtils.scale(holder.imageView, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
        } else {
            AniUtils.scale(holder.imageView, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
        }

        // redraw after the scale animation completes
        long delayMs = AniUtils.Duration.SHORT.toMillis(mContext);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        }, delayMs);

        if (mCallback != null) {
            mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
        }
    }

    private void toggleItemSelected(GridViewHolder holder, int position) {
        if (mCursor == null || !isValidPosition(position)) {
            return;
        }
        mCursor.moveToPosition(position);
        int columnIndex = mCursor.getColumnIndex(MediaModelTable.ID);
        if (columnIndex != -1) {
            int localMediaId = mCursor.getInt(columnIndex);
            boolean isSelected = mSelectedItems.contains(localMediaId);
            setItemSelectedByPosition(holder, position, !isSelected);
        }
    }

    public void setSelectedItems(ArrayList<Integer> selectedItems) {
        mSelectedItems = selectedItems;
        if (mCallback != null) {
            mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
        }
        notifyDataSetChanged();
    }
}
