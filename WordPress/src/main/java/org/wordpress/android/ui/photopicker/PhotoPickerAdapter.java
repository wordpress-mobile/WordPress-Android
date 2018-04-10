package org.wordpress.android.ui.photopicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static org.wordpress.android.ui.photopicker.PhotoPickerFragment.NUM_COLUMNS;

class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ThumbnailViewHolder> {
    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;

    /*
     * used by this adapter to communicate with the owning fragment
     */
    interface PhotoPickerAdapterListener {
        void onSelectedCountChanged(int count);

        void onAdapterLoaded(boolean isEmpty);
    }

    private class PhotoPickerItem {
        private long mId;
        private Uri mUri;
        private boolean mIsVideo;
    }

    private final ArrayList<Integer> mSelectedItems = new ArrayList<>();

    private final Context mContext;
    private RecyclerView mRecycler;
    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsListTaskRunning;
    private boolean mDisableImageReset;
    private boolean mLoadThumbnails = true;

    private final ThumbnailLoader mThumbnailLoader;
    private final PhotoPickerAdapterListener mListener;
    private final LayoutInflater mInflater;
    private final MediaBrowserType mBrowserType;

    private final ArrayList<PhotoPickerItem> mMediaList = new ArrayList<>();

    PhotoPickerAdapter(Context context,
                              MediaBrowserType browserType,
                              PhotoPickerAdapterListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        mBrowserType = browserType;
        mThumbnailLoader = new ThumbnailLoader(context);

        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecycler = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecycler = null;
    }

    void refresh(boolean forceReload) {
        if (mIsListTaskRunning) {
            AppLog.w(AppLog.T.MEDIA, "photo picker > build list task already running");
            return;
        }

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        int thumbWidth = displayWidth / NUM_COLUMNS;
        int thumbHeight = (int) (thumbWidth * 0.75f);
        boolean sizeChanged = thumbWidth != mThumbWidth || thumbHeight != mThumbHeight;

        // if thumb sizes have changed (due to device rotation, or never being set), we must
        // reload from scratch - otherwise we can do a refresh so the adapter is only loaded
        // if there are changes
        boolean mustReload;
        if (sizeChanged) {
            mThumbWidth = thumbWidth;
            mThumbHeight = thumbHeight;
            mustReload = true;
        } else {
            mustReload = forceReload;
        }

        new BuildDeviceMediaListTask(mustReload).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    @Override
    public long getItemId(int position) {
        if (isValidPosition(position)) {
            return getItemAtPosition(position).mId;
        } else {
            return NO_POSITION;
        }
    }

    private boolean isEmpty() {
        return mMediaList.size() == 0;
    }

    void setLoadThumbnails(boolean loadThumbnails) {
        if (loadThumbnails != mLoadThumbnails) {
            mLoadThumbnails = loadThumbnails;
            AppLog.d(AppLog.T.MEDIA, "PhotoPickerAdapter > loadThumbnails = " + loadThumbnails);
            if (mLoadThumbnails) {
                notifyDataSetChangedInternal();
            }
        }
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_picker_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        boolean isSelected = isItemSelected(position);
        holder.mTxtSelectionCount.setSelected(isSelected);
        if (canMultiselect()) {
            if (isSelected) {
                int count = mSelectedItems.indexOf(position) + 1;
                holder.mTxtSelectionCount.setText(String.format(Locale.getDefault(), "%d", count));
            } else {
                holder.mTxtSelectionCount.setText(null);
            }
        }

        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.mImgThumbnail.getScaleX() != scale) {
            holder.mImgThumbnail.setScaleX(scale);
            holder.mImgThumbnail.setScaleY(scale);
        }

        holder.mVideoOverlay.setVisibility(item.mIsVideo ? View.VISIBLE : View.GONE);

        if (!mDisableImageReset) {
            holder.mImgThumbnail.setImageDrawable(null);
        }

        if (mLoadThumbnails) {
            boolean animate = !mDisableImageReset;
            mThumbnailLoader.loadThumbnail(
                    holder.mImgThumbnail,
                    item.mId,
                    item.mIsVideo,
                    animate,
                    mThumbWidth);
        }
    }

    @Override
    public void onViewRecycled(ThumbnailViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mImgThumbnail.setImageDrawable(null);
        holder.mImgThumbnail.setTag(null);
    }

    private PhotoPickerItem getItemAtPosition(int position) {
        if (!isValidPosition(position)) {
            AppLog.w(AppLog.T.POSTS, "photo picker > invalid position in getItemAtPosition");
            return null;
        }
        return mMediaList.get(position);
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < mMediaList.size();
    }

    private boolean isItemSelected(int position) {
        return mSelectedItems.contains(position);
    }

    private void toggleItemSelected(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private void setItemSelected(int position, boolean isSelected) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        if (isSelected) {
            // if an item is already selected and multiselect isn't allowed, deselect the previous selection
            if (!canMultiselect() && !mSelectedItems.isEmpty()) {
                ThumbnailViewHolder prevHolder = getViewHolderAtPosition(mSelectedItems.get(0));
                if (prevHolder != null) {
                    AniUtils.scale(prevHolder.mImgThumbnail, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
                }
                mSelectedItems.clear();
            }
            mSelectedItems.add(position);
        } else {
            int selectedIndex = mSelectedItems.indexOf(position);
            if (selectedIndex > -1) {
                mSelectedItems.remove(selectedIndex);
            }
        }

        ThumbnailViewHolder holder = getViewHolderAtPosition(position);
        if (holder != null) {
            if (isSelected && canMultiselect()) {
                int count = mSelectedItems.indexOf(position) + 1;
                holder.mTxtSelectionCount.setText(String.format(Locale.getDefault(), "%d", count));
            } else if (!isSelected) {
                holder.mTxtSelectionCount.setText(null);
            }

            holder.mTxtSelectionCount.setSelected(isSelected);

            // animate the count
            AniUtils.startAnimation(holder.mTxtSelectionCount, R.anim.pop);

            // scale the thumbnail
            if (isSelected) {
                AniUtils.scale(holder.mImgThumbnail, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
            } else {
                AniUtils.scale(holder.mImgThumbnail, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
            }
        }

        if (mListener != null) {
            mListener.onSelectedCountChanged(getNumSelected());
        }

        // redraw the grid after the scale animation completes
        long delayMs = AniUtils.Duration.SHORT.toMillis(mContext);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChangedInternal();
            }
        }, delayMs);
    }

    private ThumbnailViewHolder getViewHolderAtPosition(int position) {
        if (mRecycler == null || !isValidPosition(position)) {
            return null;
        }
        return (ThumbnailViewHolder) mRecycler.findViewHolderForAdapterPosition(position);
    }

    @NonNull
    ArrayList<Uri> getSelectedURIs() {
        ArrayList<Uri> uriList = new ArrayList<>();
        for (Integer position : mSelectedItems) {
            PhotoPickerItem item = getItemAtPosition(position);
            if (item != null) {
                uriList.add(item.mUri);
            }
        }
        return uriList;
    }

    private boolean canMultiselect() {
        return mBrowserType.canMultiselect();
    }

    int getNumSelected() {
        return mSelectedItems.size();
    }

    /*
     * wrapper for notifyDataSetChanged() that prevents/reduces flicker
     */
    private void notifyDataSetChangedInternal() {
        mDisableImageReset = true;
        notifyDataSetChanged();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDisableImageReset = false;
            }
        }, 500);
    }

    /*
     * ViewHolder containing a device thumbnail
     */
    class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mImgThumbnail;
        private final TextView mTxtSelectionCount;
        private final ImageView mVideoOverlay;

        ThumbnailViewHolder(View view) {
            super(view);

            mImgThumbnail = view.findViewById(R.id.image_thumbnail);
            mTxtSelectionCount = view.findViewById(R.id.text_selection_count);
            mVideoOverlay = view.findViewById(R.id.image_video_overlay);

            mImgThumbnail.getLayoutParams().width = mThumbWidth;
            mImgThumbnail.getLayoutParams().height = mThumbHeight;

            mImgThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        toggleItemSelected(position);
                    }
                }
            });

            mImgThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    showPreview(position);
                    return true;
                }
            });

            mVideoOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    showPreview(position);
                }
            });

            ViewUtils.addCircularShadowOutline(mTxtSelectionCount);
        }
    }

    private void showPreview(int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item != null) {
            trackOpenPreviewScreenEvent(item);
            MediaPreviewActivity.showPreview(
                    mContext,
                    null,
                    item.mUri.toString());
        }
    }

    private void trackOpenPreviewScreenEvent(final PhotoPickerItem item) {
        if (item == null) {
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                Map<String, Object> properties =
                        AnalyticsUtils.getMediaProperties(mContext, item.mIsVideo, item.mUri, null);
                properties.put("is_video", item.mIsVideo);
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED, properties);
            }
        }).start();
    }

    /*
     * builds the list of media items from the device
     */
    private class BuildDeviceMediaListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoPickerItem> mTmpList = new ArrayList<>();
        private final boolean mReload;
        private static final String ID_COL = MediaStore.Images.Media._ID;

        BuildDeviceMediaListTask(boolean mustReload) {
            super();
            mReload = mustReload;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // images
            addMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false);

            // videos
            if (!mBrowserType.isSingleImagePicker()) {
                addMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true);
            }

            // sort by id in reverse (newest first)
            Collections.sort(mTmpList, new Comparator<PhotoPickerItem>() {
                @Override
                public int compare(PhotoPickerItem o1, PhotoPickerItem o2) {
                    long id1 = o1.mId;
                    long id2 = o2.mId;
                    return (id2 < id1) ? -1 : ((id1 == id2) ? 0 : 1);
                }
            });

            // if we're reloading then return true so the adapter is updated, otherwise only
            // return true if changes are detected
            return mReload || !isSameMediaList();
        }

        private void addMedia(Uri baseUri, boolean isVideo) {
            String[] projection = {ID_COL};
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(
                        baseUri,
                        projection,
                        null,
                        null,
                        null);
            } catch (SecurityException e) {
                AppLog.e(AppLog.T.MEDIA, e);
            }

            if (cursor == null) {
                return;
            }

            try {
                int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
                while (cursor.moveToNext()) {
                    PhotoPickerItem item = new PhotoPickerItem();
                    item.mId = cursor.getLong(idIndex);
                    item.mUri = Uri.withAppendedPath(baseUri, "" + item.mId);
                    item.mIsVideo = isVideo;
                    mTmpList.add(item);
                }
            } finally {
                SqlUtils.closeCursor(cursor);
            }
        }

        // returns true if the media list built here is the same as the existing one
        private boolean isSameMediaList() {
            if (mTmpList.size() != mMediaList.size()) {
                return false;
            }
            for (int i = 0; i < mTmpList.size(); i++) {
                if (!isValidPosition(i)) {
                    return false;
                }
                if (mTmpList.get(i).mId != mMediaList.get(i).mId) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsListTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsListTaskRunning = false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMediaList.clear();
                mMediaList.addAll(mTmpList);
                notifyDataSetChanged();
            }
            if (mListener != null) {
                mListener.onAdapterLoaded(isEmpty());
            }
            mIsListTaskRunning = false;
        }
    }
}
