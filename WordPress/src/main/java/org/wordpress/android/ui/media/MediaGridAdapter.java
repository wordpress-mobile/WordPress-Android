package org.wordpress.android.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.utils.AuthenticationUtils;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotoPickerUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.ViewUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/**
 * An adapter for the media gallery grid.
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.GridViewHolder> {
    private MediaGridAdapterCallback mCallback;
    private final MediaBrowserType mBrowserType;

    private boolean mHasRetrievedAll;
    private boolean mInMultiSelect;
    private boolean mLoadThumbnails = true;

    private final Handler mHandler;
    private final LayoutInflater mInflater;

    private final Context mContext;
    private final SiteModel mSite;

    private final ArrayList<MediaModel> mMediaList = new ArrayList<>();
    private final ArrayList<Integer> mSelectedItems = new ArrayList<>();

    private final int mThumbWidth;
    private final int mThumbHeight;

    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;

    @Inject ImageManager mImageManager;
    @Inject AuthenticationUtils mAuthenticationUtils;

    public interface MediaGridAdapterCallback {
        void onAdapterFetchMoreData();

        void onAdapterItemClicked(int position, boolean isLongClick);

        void onAdapterSelectionCountChanged(int count);

        void onAdapterRequestRetry(int position);

        void onAdapterRequestDelete(int position);
    }

    private static final int INVALID_POSITION = -1;

    public MediaGridAdapter(@NonNull Context context, @NonNull SiteModel site, @NonNull MediaBrowserType browserType) {
        super();
        ((WordPress) WordPress.getContext().getApplicationContext()).component().inject(this);
        setHasStableIds(true);

        mContext = context;
        mSite = site;
        mBrowserType = browserType;
        mInflater = LayoutInflater.from(context);
        mHandler = new Handler();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        mThumbWidth = displayWidth / getColumnCount(mContext);
        mThumbHeight = (int) (mThumbWidth * 0.75f);
    }

    @Override
    public long getItemId(int position) {
        return getLocalMediaIdAtPosition(position);
    }

    public void setMediaList(@NonNull List<MediaModel> mediaList) {
        if (!isSameList(mediaList)) {
            mMediaList.clear();
            mMediaList.addAll(mediaList);
            notifyDataSetChanged();
        }
    }

    @Override
    public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.media_grid_item, parent, false);
        return new GridViewHolder(view);
    }

    /*
     * returns the most optimal url to use when retrieving a media image for display here
     */
    private String getBestImageUrl(@NonNull MediaModel media) {
        // return photon-ized url if the site allows it since this gives us the image at the
        // exact size we need here
        if (SiteUtils.isPhotonCapable(mSite)) {
            return PhotonUtils.getPhotonImageUrl(media.getUrl(), mThumbWidth, mThumbHeight,
                    mSite.isPrivateWPComAtomic());
        }

        // can't use photon, so try the various image sizes - note we favor medium-large and
        // medium because they're more bandwidth-friendly than large
        if (!TextUtils.isEmpty(media.getFileUrlMediumLargeSize())) {
            return media.getFileUrlMediumLargeSize();
        } else if (!TextUtils.isEmpty(media.getFileUrlMediumSize())) {
            return media.getFileUrlMediumSize();
        } else if (!TextUtils.isEmpty(media.getFileUrlLargeSize())) {
            return media.getFileUrlLargeSize();
        }

        // next stop is to return the thumbnail, which will look pixelated in the grid but it's
        // better than eating bandwidth showing the full-sized image
        if (!TextUtils.isEmpty(media.getThumbnailUrl())) {
            return media.getThumbnailUrl();
        }

        // last resort, return the full-sized image url
        return UrlUtils.removeQuery(media.getUrl());
    }

    @Override
    public void onBindViewHolder(GridViewHolder holder, int position) {
        if (!isValidPosition(position)) {
            return;
        }

        MediaModel media = mMediaList.get(position);

        String strState = media.getUploadState();
        MediaUploadState state = MediaUploadState.fromString(strState);

        boolean isLocalFile = MediaUtils.isLocalFile(strState) && !TextUtils.isEmpty(media.getFilePath());
        boolean isSelected = isItemSelected(media.getId());
        boolean canSelect = canSelectPosition(position);
        boolean isImage = media.getMimeType() != null && media.getMimeType().startsWith("image/");

        if (!mLoadThumbnails) {
            holder.mFileContainer.setVisibility(View.GONE);
            mImageManager.load(holder.mImageView, ImageType.PHOTO, "", ScaleType.CENTER_CROP);
        } else if (isImage) {
            holder.mFileContainer.setVisibility(View.GONE);
            if (isLocalFile) {
                mImageManager.load(holder.mImageView, ImageType.PHOTO, media.getFilePath(), ScaleType.CENTER_CROP);
            } else {
                mImageManager.load(holder.mImageView, ImageType.PHOTO, getBestImageUrl(media), ScaleType.CENTER_CROP);
            }
        } else if (media.isVideo()) {
            holder.mFileContainer.setVisibility(View.GONE);
            loadVideoThumbnail(position, media, holder.mImageView);
        } else {
            // not an image or video, so show file name and file type
            String fileName = media.getFileName();
            String title = media.getTitle();
            String fileExtension = MediaUtils.getExtensionForMimeType(media.getMimeType());
            holder.mFileContainer.setVisibility(View.VISIBLE);
            holder.mTitleView.setText(TextUtils.isEmpty(title) ? fileName : title);
            holder.mFileTypeView.setText(fileExtension.toUpperCase(Locale.ROOT));
            int placeholderResId = WPMediaUtils.getPlaceholder(fileName);
            ColorUtils.INSTANCE.setImageResourceWithTint(holder.mFileTypeImageView, placeholderResId,
                    R.color.neutral_30);
            mImageManager.cancelRequestAndClearImageView(holder.mImageView);
        }
        holder.mImageView.setContentDescription(mContext.getString(R.string.media_grid_item_image_desc,
                StringUtils.notNullStr(media.getFileName())));

        if (mBrowserType.canMultiselect() && canSelect) {
            holder.mSelectionCountContainer.setVisibility(View.VISIBLE);
            holder.mSelectionCountTextView.setVisibility(View.VISIBLE);
            holder.mSelectionCountTextView.setSelected(isSelected);
            if (isSelected) {
                int count = mSelectedItems.indexOf(media.getId()) + 1;
                holder.mSelectionCountTextView.setText(String.format(Locale.getDefault(), "%d", count));
            } else {
                holder.mSelectionCountTextView.setText(null);
            }
        } else {
            holder.mSelectionCountContainer.setVisibility(View.GONE);
            holder.mSelectionCountTextView.setVisibility(View.GONE);
        }

        // make sure the thumbnail scale reflects its selection state
        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.mImageView.getScaleX() != scale) {
            holder.mImageView.setScaleX(scale);
            holder.mImageView.setScaleY(scale);
        }

        // show upload state unless it's already uploaded
        if (state != MediaUploadState.UPLOADED) {
            holder.mStateContainer.setVisibility(View.VISIBLE);

            // only show progress for items currently being uploaded or deleted
            boolean showProgress = state == MediaUploadState.UPLOADING || state == MediaUploadState.DELETING;
            holder.mProgressUpload.setVisibility(showProgress ? View.VISIBLE : View.GONE);

            // failed uploads can be retried or deleted, queued items can be deleted
            if (state == MediaUploadState.FAILED || state == MediaUploadState.QUEUED) {
                holder.mRetryDeleteContainer.setVisibility(View.VISIBLE);
                holder.mImgRetry.setVisibility(state == MediaUploadState.FAILED ? View.VISIBLE : View.GONE);
            } else {
                holder.mRetryDeleteContainer.setVisibility(View.GONE);
            }
            holder.mStateTextView.setText(getLabelForMediaUploadState(state));

            // hide the video player icon so it doesn't overlap state label
            holder.mVideoOverlayContainer.setVisibility(View.GONE);
        } else {
            holder.mStateContainer.setVisibility(View.GONE);
            holder.mStateContainer.setOnClickListener(null);
            holder.mVideoOverlayContainer.setVisibility(media.isVideo() ? View.VISIBLE : View.GONE);
        }

        // if we are near the end, make a call to fetch more
        if (position == getItemCount() - 1
            && !mHasRetrievedAll
            && mCallback != null) {
            mCallback.onAdapterFetchMoreData();
        }
    }

    @Override
    public void onViewRecycled(GridViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mImageView.setTag(R.id.media_grid_file_path_id, null);
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final ImageView mImageView;
        private final TextView mFileTypeView;
        private final ImageView mFileTypeImageView;
        private final TextView mSelectionCountTextView;
        private final TextView mStateTextView;
        private final ProgressBar mProgressUpload;
        private final ViewGroup mStateContainer;
        private final ViewGroup mFileContainer;
        private final ViewGroup mVideoOverlayContainer;
        private final ViewGroup mSelectionCountContainer;
        private final ViewGroup mRetryDeleteContainer;
        private final ImageView mImgRetry;
        private final ImageView mImgTrash;

        GridViewHolder(View view) {
            super(view);

            mImageView = view.findViewById(R.id.media_grid_item_image);
            mSelectionCountTextView = view.findViewById(R.id.text_selection_count);

            mStateContainer = view.findViewById(R.id.media_grid_item_upload_state_container);
            mStateTextView = mStateContainer.findViewById(R.id.media_grid_item_upload_state);
            mProgressUpload = mStateContainer.findViewById(R.id.media_grid_item_upload_progress);

            mFileContainer = view.findViewById(R.id.media_grid_item_file_container);
            mTitleView = mFileContainer.findViewById(R.id.media_grid_item_name);
            mFileTypeView = mFileContainer.findViewById(R.id.media_grid_item_filetype);
            mFileTypeImageView = mFileContainer.findViewById(R.id.media_grid_item_filetype_image);

            mVideoOverlayContainer = view.findViewById(R.id.frame_video_overlay);
            mSelectionCountContainer = view.findViewById(R.id.frame_selection_count);

            // make the progress bar white
            mProgressUpload.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

            mRetryDeleteContainer = view.findViewById(R.id.container_retry_delete);
            mImgRetry = view.findViewById(R.id.image_retry);
            mImgTrash = view.findViewById(R.id.image_trash);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    doAdapterItemClicked(position, false);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    doAdapterItemClicked(position, true);
                    return true;
                }
            });
            ViewUtilsKt.redirectContextClickToLongPressListener(itemView);

            mSelectionCountContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (canSelectPosition(position)) {
                        setInMultiSelect(true);
                        toggleItemSelected(GridViewHolder.this, position);
                    }
                }
            });

            mImgRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position) && mCallback != null) {
                        mCallback.onAdapterRequestRetry(position);
                    }
                }
            });

            mImgTrash.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position) && mCallback != null) {
                        mCallback.onAdapterRequestDelete(position);
                    }
                }
            });

            ViewUtils.addCircularShadowOutline(mSelectionCountTextView);
            addImageSelectedToAccessibilityFocusedEvent(mImageView);
        }

        private void addImageSelectedToAccessibilityFocusedEvent(ImageView imageView) {
            AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(imageView, event -> {
                int position = getAdapterPosition();
                if (isValidPosition(position)) {
                    if (isItemSelectedByPosition(position)) {
                        final String imageSelectedText = imageView.getContext().getString(
                                R.string.photo_picker_image_selected);
                        if (!imageView.getContentDescription().toString().contains(imageSelectedText)) {
                            imageView.setContentDescription(
                                    imageView.getContentDescription() + " "
                                    + imageSelectedText);
                        }
                    }
                }
            });
        }

        private void doAdapterItemClicked(int position, boolean isLongClick) {
            if (!isValidPosition(position)) {
                return;
            }
            if (isInMultiSelect() && !isLongClick) {
                if (canSelectPosition(position)) {
                    toggleItemSelected(GridViewHolder.this, position);
                }
            } else {
                if (mBrowserType.canMultiselect() && canSelectPosition(position) && !isLongClick) {
                    setInMultiSelect(true);
                    toggleItemSelected(GridViewHolder.this, position);
                }
                if (mCallback != null) {
                    mCallback.onAdapterItemClicked(position, isLongClick);
                }
            }
        }
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
            return mMediaList.get(position).getId();
        }
        AppLog.w(AppLog.T.MEDIA, "MediaGridAdapter > Invalid position " + position);
        return INVALID_POSITION;
    }

    /*
     * determines whether the media item at the passed position can be selected - not allowed
     * for local files or deleted items when used as a picker
     */
    private boolean canSelectPosition(int position) {
        if (!isValidPosition(position)) {
            return false;
        }
        if (mBrowserType.isPicker()) {
            MediaModel media = mMediaList.get(position);
            if (MediaUtils.isLocalFile(media.getUploadState())) {
                return false;
            }
            MediaUploadState state = MediaUploadState.fromString(media.getUploadState());
            return state != MediaUploadState.DELETING && state != MediaUploadState.DELETED;
        } else {
            return true;
        }
    }

    /*
     * loads the thumbnail for the passed video media item - works with both local and network videos
     */
    private void loadVideoThumbnail(final int position, final @NonNull MediaModel media,
                                    @NonNull final ImageView imageView) {
        // if we have a thumbnail url, use it and be done
        if (!TextUtils.isEmpty(media.getThumbnailUrl()) && !MediaUtils.isVideo(media.getThumbnailUrl())) {
            mImageManager.load(imageView, ImageType.VIDEO, media.getThumbnailUrl(), ScaleType.CENTER_CROP);
            return;
        }

        // thumbnail url is empty, so either this is a local (still uploading) video or the server simply
        // hasn't supplied the thumbnail url
        final String filePath;
        if (!TextUtils.isEmpty(media.getFilePath()) && new File(media.getFilePath()).exists()) {
            filePath = media.getFilePath();
        } else {
            filePath = media.getUrl();
        }

        if (TextUtils.isEmpty(filePath)) {
            AppLog.w(AppLog.T.MEDIA, "MediaGridAdapter > No path to video thumbnail");
            return;
        }
        imageView.setTag(R.id.media_grid_file_path_id, filePath);
        // see if we have a cached thumbnail before retrieving it
        Bitmap bitmap = WordPress.getBitmapCache().get(filePath);
        if (bitmap != null) {
            mImageManager.load(imageView, bitmap, ScaleType.CENTER_CROP);
            return;
        }

        new Thread() {
            @Override
            public void run() {
                final Bitmap thumb =
                        ImageUtils.getVideoFrameFromVideo(filePath,
                                mThumbWidth,
                                mAuthenticationUtils.getAuthHeaders(filePath)
                        );
                if (thumb != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            WordPress.getBitmapCache().put(filePath, thumb);
                            if (imageView.getTag(R.id.media_grid_file_path_id) instanceof String
                                && (imageView.getTag(R.id.media_grid_file_path_id)).equals(filePath)) {
                                imageView.setTag(R.id.media_grid_file_path_id, null);
                                notifyItemChanged(position);
                            }
                        }
                    });
                }
            }
        }.start();
    }

    public boolean isEmpty() {
        return mMediaList.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    public static int getColumnCount(Context context) {
        return DisplayUtils.isLandscape(context) ? 4 : 3;
    }

    public void setCallback(MediaGridAdapterCallback callback) {
        mCallback = callback;
    }

    public void setHasRetrievedAll(boolean b) {
        mHasRetrievedAll = b;
    }

    void setLoadThumbnails(boolean loadThumbnails) {
        if (loadThumbnails != mLoadThumbnails) {
            mLoadThumbnails = loadThumbnails;
            AppLog.d(AppLog.T.MEDIA, "MediaGridAdapter > loadThumbnails = " + loadThumbnails);
            if (mLoadThumbnails) {
                notifyDataSetChanged();
            }
        }
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

    public void removeSelectionByLocalId(int localMediaId) {
        if (isItemSelected(localMediaId)) {
            mSelectedItems.remove(Integer.valueOf(localMediaId));
            if (mCallback != null) {
                mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
            }
            notifyDataSetChanged();
        }
    }

    private void setItemSelectedByPosition(GridViewHolder holder, int position, boolean selected) {
        if (!isValidPosition(position)) {
            return;
        }

        int localMediaId = mMediaList.get(position).getId();
        if (selected) {
            mSelectedItems.add(localMediaId);
        } else {
            mSelectedItems.remove(Integer.valueOf(localMediaId));
        }

        // show and animate the count
        if (selected) {
            holder.mSelectionCountTextView
                    .setText(String.format(Locale.getDefault(), "%d", mSelectedItems.indexOf(localMediaId) + 1));
        } else {
            holder.mSelectionCountTextView.setText(null);
        }
        AniUtils.startAnimation(holder.mSelectionCountContainer, R.anim.pop);
        holder.mSelectionCountTextView.setVisibility(selected ? View.VISIBLE : View.GONE);

        // scale the thumbnail
        if (selected) {
            AniUtils.scale(holder.mImageView, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
        } else {
            AniUtils.scale(holder.mImageView, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
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

        PhotoPickerUtils.announceSelectedImageForAccessibility(holder.mImageView, selected);
    }

    private void toggleItemSelected(GridViewHolder holder, int position) {
        if (!isValidPosition(position)) {
            return;
        }
        boolean isSelected = isItemSelectedByPosition(position);
        setItemSelectedByPosition(holder, position, !isSelected);
    }

    private boolean isItemSelectedByPosition(int position) {
        int localMediaId = mMediaList.get(position).getId();
        return mSelectedItems.contains(localMediaId);
    }

    public void setSelectedItems(ArrayList<Integer> selectedItems) {
        mSelectedItems.clear();
        mSelectedItems.addAll(selectedItems);
        if (mCallback != null) {
            mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
        }
        notifyDataSetChanged();
    }

    private String getLabelForMediaUploadState(MediaUploadState uploadState) {
        switch (uploadState) {
            case QUEUED:
                return mContext.getString(R.string.media_upload_state_queued);
            case UPLOADING:
                return mContext.getString(R.string.media_upload_state_uploading);
            case DELETING:
                return mContext.getString(R.string.media_upload_state_deleting);
            case DELETED:
                return mContext.getString(R.string.media_upload_state_deleted);
            case FAILED:
                return mContext.getString(R.string.media_upload_state_failed);
            case UPLOADED:
                return mContext.getString(R.string.media_upload_state_uploaded);
        }
        return "";
    }

    void updateMediaItem(@NonNull MediaModel media, boolean forceUpdate) {
        int index = indexOfMedia(media);
        if (index > -1 && (forceUpdate || !media.equals(mMediaList.get(index)))) {
            mMediaList.set(index, media);
            notifyItemChanged(index);
        }
    }

    void removeMediaItem(@NonNull MediaModel media) {
        int index = indexOfMedia(media);
        if (index > -1) {
            mMediaList.remove(index);
            notifyItemRemoved(index);
        }
    }

    boolean mediaExists(@NonNull MediaModel media) {
        return indexOfMedia(media) > -1;
    }

    private int indexOfMedia(@NonNull MediaModel media) {
        for (int i = 0; i < mMediaList.size(); i++) {
            if (media.getId() == mMediaList.get(i).getId()) {
                return i;
            }
        }
        return -1;
    }

    /*
     * returns true if the passed list is the same as the existing one
     */
    private boolean isSameList(@NonNull List<MediaModel> otherList) {
        if (otherList.size() != mMediaList.size()) {
            return false;
        }

        for (MediaModel otherMedia : otherList) {
            if (!mediaExists(otherMedia)) {
                return false;
            }
        }

        return true;
    }
}
