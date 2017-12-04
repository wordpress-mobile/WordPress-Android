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
        void onItemTapped(Uri mediaUri);
        void onSelectedCountChanged(int count);
        void onAdapterLoaded(boolean isEmpty);
    }

    private class PhotoPickerItem {
        private long _id;
        private Uri uri;
        private boolean isVideo;
    }

    class UriList extends ArrayList<Uri> {
        private int indexOfUri(Uri imageUri) {
            for (int i = 0; i < size(); i++) {
                if (get(i).equals(imageUri)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private final UriList mSelectedUris = new UriList();

    private final Context mContext;
    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsMultiSelectEnabled;
    private boolean mIsListTaskRunning;
    private boolean mDisableImageReset;
    private boolean mLoadThumbnails = true;

    private final ThumbnailLoader mThumbnailLoader;
    private final PhotoPickerAdapterListener mListener;
    private final LayoutInflater mInflater;
    private final MediaBrowserType mBrowserType;

    private final ArrayList<PhotoPickerItem> mMediaList = new ArrayList<>();

    public PhotoPickerAdapter(Context context,
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
            return getItemAtPosition(position)._id;
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

        int selectedIndex = mIsMultiSelectEnabled ? mSelectedUris.indexOfUri(item.uri) : -1;
        if (mBrowserType.canMultiselect()) {
            if (selectedIndex > -1) {
                holder.txtSelectionCount.setSelected(true);
                holder.txtSelectionCount.setText(Integer.toString(selectedIndex + 1));
            } else {
                holder.txtSelectionCount.setSelected(false);
                holder.txtSelectionCount.setText(null);
            }
        } else {
            holder.txtSelectionCount.setVisibility(View.GONE);
        }

        holder.videoOverlay.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

        if (!mDisableImageReset) {
            holder.imgThumbnail.setImageDrawable(null);
        }

        if (mLoadThumbnails) {
            boolean animate = !mDisableImageReset;
            mThumbnailLoader.loadThumbnail(
                    holder.imgThumbnail,
                    item._id,
                    item.isVideo,
                    animate,
                    mThumbWidth);
        }
    }

    @Override
    public void onViewRecycled(ThumbnailViewHolder holder) {
        super.onViewRecycled(holder);
        holder.imgThumbnail.setImageDrawable(null);
        holder.imgThumbnail.setTag(null);
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

    void setMultiSelectEnabled(boolean enabled) {
        if (mIsMultiSelectEnabled == enabled) return;

        mIsMultiSelectEnabled = enabled;

        if (!enabled && mSelectedUris.size() > 0) {
            mSelectedUris.clear();
            notifyDataSetChangedInternal();
        }
    }

    /*
     * toggles the selection state of the item at the passed position
     */
    private void toggleSelection(ThumbnailViewHolder holder, int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        boolean isSelected;
        int selectedIndex = mSelectedUris.indexOfUri(item.uri);
        if (selectedIndex > -1) {
            mSelectedUris.remove(selectedIndex);
            isSelected = false;
            holder.txtSelectionCount.setText(null);
        } else {
            mSelectedUris.add(item.uri);
            isSelected = true;
            holder.txtSelectionCount.setText(Integer.toString(mSelectedUris.size()));
        }
        holder.txtSelectionCount.setSelected(isSelected);

        // animate the count
        AniUtils.startAnimation(holder.txtSelectionCount, R.anim.pop);

        // scale the thumbnail
        if (isSelected) {
            AniUtils.scale(holder.imgThumbnail, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
        } else {
            AniUtils.scale(holder.imgThumbnail, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
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

    @NonNull
    ArrayList<Uri> getSelectedURIs() {
        //noinspection unchecked
        return (ArrayList<Uri>)mSelectedUris.clone();
    }

    int getNumSelected() {
        return mIsMultiSelectEnabled ? mSelectedUris.size() : 0;
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
        private final ImageView imgThumbnail;
        private final TextView txtSelectionCount;
        private final ImageView videoOverlay;

        public ThumbnailViewHolder(View view) {
            super(view);

            imgThumbnail = (ImageView) view.findViewById(R.id.image_thumbnail);
            txtSelectionCount = (TextView) view.findViewById(R.id.text_selection_count);
            videoOverlay = (ImageView) view.findViewById(R.id.image_video_overlay);

            imgThumbnail.getLayoutParams().width = mThumbWidth;
            imgThumbnail.getLayoutParams().height = mThumbHeight;

            imgThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        if (mBrowserType.canMultiselect()) {
                            if (!mIsMultiSelectEnabled) {
                                setMultiSelectEnabled(true);
                            }
                            toggleSelection(ThumbnailViewHolder.this, position);
                        } else if (mListener != null) {
                            Uri uri = getItemAtPosition(position).uri;
                            mListener.onItemTapped(uri);
                        }
                    }
                }
            });

            imgThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    showPreview(position);
                    return true;
                }
            });

            videoOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    showPreview(position);
                }
            });

            ViewUtils.addCircularShadowOutline(txtSelectionCount);
        }
    }

    private void showPreview(int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item != null) {
            trackOpenPreviewScreenEvent(item);
            MediaPreviewActivity.showPreview(
                    mContext,
                    null,
                    item.uri.toString());
        }
    }

    private void trackOpenPreviewScreenEvent(final PhotoPickerItem item) {
        if (item == null) {
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                Map<String, Object> properties = AnalyticsUtils.getMediaProperties(mContext, item.isVideo, item.uri, null);
                properties.put("is_video", item.isVideo);
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED, properties);
            }
        }).start();
    }

    /*
     * builds the list of media items from the device
     */
    private class BuildDeviceMediaListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoPickerItem> tmpList = new ArrayList<>();
        private final boolean reload;
        private static final String ID_COL = MediaStore.Images.Media._ID;

        BuildDeviceMediaListTask(boolean mustReload) {
            super();
            reload = mustReload;
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
            Collections.sort(tmpList, new Comparator<PhotoPickerItem>() {
                @Override
                public int compare(PhotoPickerItem o1, PhotoPickerItem o2) {
                    long id1 = o1._id;
                    long id2 = o2._id;
                    return (id2 < id1) ? -1 : ((id1 == id2) ? 0 : 1);
                }
            });

            // if we're reloading then return true so the adapter is updated, otherwise only
            // return true if changes are detected
            return reload || !isSameMediaList();
        }

        private void addMedia(Uri baseUri, boolean isVideo) {
            String[] projection = { ID_COL };
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
                    item._id = cursor.getLong(idIndex);
                    item.uri = Uri.withAppendedPath(baseUri, "" + item._id);
                    item.isVideo = isVideo;
                    tmpList.add(item);
                }
            } finally {
                SqlUtils.closeCursor(cursor);
            }
        }

        // returns true if the media list built here is the same as the existing one
        private boolean isSameMediaList() {
            if (tmpList.size() != mMediaList.size()) {
                return false;
            }
            for (int i = 0; i < tmpList.size(); i++) {
                if (!isValidPosition(i)) {
                    return false;
                }
                if (tmpList.get(i)._id != mMediaList.get(i)._id) {
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
                mMediaList.addAll(tmpList);
                notifyDataSetChanged();
            }
            if (mListener != null) {
                mListener.onAdapterLoaded(isEmpty());
            }
            mIsListTaskRunning = false;
        }
    }
}
