package org.wordpress.android.ui.photopicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotoPickerUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.ViewUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.wordpress.android.ui.photopicker.PhotoPickerFragment.NUM_COLUMNS;

public class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ThumbnailViewHolder> {
    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;

    /*
     * used by this adapter to communicate with the owning fragment
     */
    interface PhotoPickerAdapterListener {
        void onSelectedCountChanged(int count);

        void onAdapterLoaded(boolean isEmpty);

        void onItemSelected(boolean isVideo);
    }

    private class PhotoPickerItem {
        private long mId;
        private Uri mUri;
        private boolean mIsVideo;
    }

    private final ArrayList<Integer> mSelectedPositions = new ArrayList<>();
    private static final AniUtils.Duration ANI_DURATION = AniUtils.Duration.SHORT;

    private final Context mContext;
    private RecyclerView mRecycler;
    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsListTaskRunning;
    private boolean mLoadThumbnails = true;

    private final PhotoPickerAdapterListener mListener;
    private final LayoutInflater mInflater;
    private final MediaBrowserType mBrowserType;

    private final ArrayList<PhotoPickerItem> mMediaList = new ArrayList<>();

    protected final ImageManager mImageManager;

    PhotoPickerAdapter(Context context,
                       MediaBrowserType browserType,
                       PhotoPickerAdapterListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        mBrowserType = browserType;
        mImageManager = ImageManager.getInstance();

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
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_picker_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    private void updateSelectionCountForPosition(int position,
                                                 boolean isSelected,
                                                 @NonNull TextView txtSelectionCount) {
        if (canMultiselect() && isSelected) {
            int count = mSelectedPositions.indexOf(position) + 1;
            txtSelectionCount.setText(String.format(Locale.getDefault(), "%d", count));
        } else {
            txtSelectionCount.setText(null);
        }
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        boolean isSelected = isItemSelected(position);
        holder.mTxtSelectionCount.setSelected(isSelected);
        holder.mTxtSelectionCount.setVisibility(isSelected || canMultiselect() ? View.VISIBLE : View.GONE);
        updateSelectionCountForPosition(position, isSelected, holder.mTxtSelectionCount);

        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.mImgThumbnail.getScaleX() != scale) {
            holder.mImgThumbnail.setScaleX(scale);
            holder.mImgThumbnail.setScaleY(scale);
        }

        holder.mVideoOverlay.setVisibility(item.mIsVideo ? View.VISIBLE : View.GONE);

        if (mLoadThumbnails) {
            mImageManager.load(holder.mImgThumbnail, ImageType.PHOTO, item.mUri.toString(), ScaleType.FIT_CENTER);
        } else {
            mImageManager.cancelRequestAndClearImageView(holder.mImgThumbnail);
        }
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
        return mSelectedPositions.contains(position);
    }

    private void toggleItemSelected(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private void setItemSelected(int position, boolean isSelected) {
        setItemSelected(position, isSelected, true);
    }

    private void setItemSelected(int position, boolean isSelected, boolean updateAfter) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        // if an item is already selected and multiselect isn't allowed, deselect the previous selection
        if (isSelected && !canMultiselect() && !mSelectedPositions.isEmpty()) {
            setItemSelected(mSelectedPositions.get(0), false, false);
        }

        if (isSelected) {
            mSelectedPositions.add(position);
            if (mListener != null) {
                mListener.onItemSelected(item.mIsVideo);
            }
        } else {
            int selectedIndex = mSelectedPositions.indexOf(position);
            if (selectedIndex > -1) {
                mSelectedPositions.remove(selectedIndex);
            }
        }

        ThumbnailViewHolder holder = getViewHolderAtPosition(position);
        if (holder != null) {
            holder.mTxtSelectionCount.setSelected(isSelected);
            updateSelectionCountForPosition(position, isSelected, holder.mTxtSelectionCount);

            if (isSelected) {
                AniUtils.scale(holder.mImgThumbnail, SCALE_NORMAL, SCALE_SELECTED, ANI_DURATION);
            } else {
                AniUtils.scale(holder.mImgThumbnail, SCALE_SELECTED, SCALE_NORMAL, ANI_DURATION);
            }

            if (canMultiselect()) {
                AniUtils.startAnimation(holder.mTxtSelectionCount, R.anim.pop);
            } else if (isSelected) {
                AniUtils.fadeIn(holder.mTxtSelectionCount, ANI_DURATION);
            } else {
                AniUtils.fadeOut(holder.mTxtSelectionCount, ANI_DURATION);
            }
        }

        if (updateAfter) {
            notifySelectionCountChanged();
            // redraw the grid after the scale animation completes
            long delayMs = ANI_DURATION.toMillis(mContext);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            }, delayMs);
        }
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
        for (Integer position : mSelectedPositions) {
            PhotoPickerItem item = getItemAtPosition(position);
            if (item != null) {
                uriList.add(item.mUri);
            }
        }
        return uriList;
    }

    ArrayList<Integer> getSelectedPositions() {
        return mSelectedPositions;
    }

    void setSelectedPositions(@NonNull ArrayList<Integer> selectedPositions) {
        mSelectedPositions.clear();
        mSelectedPositions.addAll(selectedPositions);
        notifyDataSetChanged();
        notifySelectionCountChanged();
    }

    void clearSelection() {
        if (mSelectedPositions.size() > 0) {
            mSelectedPositions.clear();
            notifyDataSetChanged();
        }
    }

    private boolean canMultiselect() {
        return mBrowserType.canMultiselect();
    }

    int getNumSelected() {
        return mSelectedPositions.size();
    }

    private void notifySelectionCountChanged() {
        if (mListener != null) {
            mListener.onSelectedCountChanged(getNumSelected());
        }
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

            if (!canMultiselect()) {
                mTxtSelectionCount.setBackgroundResource(R.drawable.photo_picker_circle_pressed);
            }

            mImgThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        toggleItemSelected(position);
                        PhotoPickerUtils.announceSelectedImageForAccessibility(mImgThumbnail, isItemSelected(position));
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
            ViewUtilsKt.redirectContextClickToLongPressListener(mImgThumbnail);

            mVideoOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    showPreview(position);
                }
            });

            ViewUtils.addCircularShadowOutline(mTxtSelectionCount);
            addImageSelectedToAccessibilityFocusedEvent(mImgThumbnail);
        }

        private void addImageSelectedToAccessibilityFocusedEvent(ImageView imageView) {
            AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(imageView, event -> {
                int position = getAdapterPosition();
                if (isValidPosition(position)) {
                    String imageSelectedText = imageView.getContext()
                                                        .getString(R.string.photo_picker_image_selected);

                    if (isItemSelected(position)) {
                        if (!imageView.getContentDescription().toString().contains(imageSelectedText)) {
                            imageView.setContentDescription(
                                    imageView.getContentDescription() + " "
                                    + imageSelectedText);
                        }
                    } else {
                        imageView.setContentDescription(imageView.getContentDescription()
                                                                 .toString().replace(imageSelectedText,
                                        ""));
                    }
                }
            });
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
            if (mBrowserType.isImagePicker()) {
                addMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false);
            }

            // videos
            if (mBrowserType.isVideoPicker()) {
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
