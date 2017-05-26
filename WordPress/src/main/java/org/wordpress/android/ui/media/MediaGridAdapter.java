package org.wordpress.android.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.FadeInNetworkImageView;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * An adapter for the media gallery grid.
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.GridViewHolder> {
    private MediaGridAdapterCallback mCallback;
    private boolean mHasRetrievedAll;

    private boolean mAllowMultiselect;
    private boolean mInMultiSelect;
    private boolean mShowPreviewIcon;

    private final Handler mHandler;
    private final LayoutInflater mInflater;

    private ImageLoader mImageLoader;
    private final Context mContext;
    private final SiteModel mSite;

    private final ArrayList<MediaModel> mMediaList = new ArrayList<>();
    private final ArrayList<Integer> mSelectedItems = new ArrayList<>();

    private final int mThumbWidth;
    private final int mThumbHeight;

    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .85f;

    public interface MediaGridAdapterCallback {
        void onAdapterFetchMoreData();
        void onAdapterRetryUpload(int localMediaId);
        void onAdapterItemSelected(View sourceView, int position);
        void onAdapterSelectionCountChanged(int count);
    }

    private static final int INVALID_POSITION = -1;

    public MediaGridAdapter(Context context, SiteModel site, ImageLoader imageLoader) {
        super();
        setHasStableIds(true);

        mContext = context;
        mSite = site;
        mInflater = LayoutInflater.from(context);
        mHandler = new Handler();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        mThumbWidth = displayWidth / getColumnCount(mContext);
        mThumbHeight = (int) (mThumbWidth * 0.75f);

        setImageLoader(imageLoader);
    }

    public void setShowPreviewIcon(boolean show) {
        if (show != mShowPreviewIcon) {
            mShowPreviewIcon = show;
            if (getItemCount() > 0) {
                notifyDataSetChanged();;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return getLocalMediaIdAtPosition(position);
    }

    private void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
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

    @Override
    public void onBindViewHolder(GridViewHolder holder, int position) {
        if (!isValidPosition(position)) {
            return;
        }

        MediaModel media = mMediaList.get(position);
        holder.imageView.setTag(null);

        String strState = media.getUploadState();
        MediaUploadState state = MediaUploadState.fromString(strState);

        boolean isLocalFile = MediaUtils.isLocalFile(strState) && !TextUtils.isEmpty(media.getFilePath());
        boolean isSelected = isItemSelected(media.getId());
        boolean isImage = media.getMimeType() != null && media.getMimeType().startsWith("image/");

        if (isImage) {
            holder.fileContainer.setVisibility(View.GONE);
            if (isLocalFile) {
                loadLocalImage(media.getFilePath(), holder.imageView);
            } else {
                // if this isn't a private site use Photon to request the image at the exact size,
                // otherwise append the standard wp query params to request the desired size
                // TODO: we should drop using Photon for self-hosted sites once the media model
                // has been updated to include the various image sizes
                String thumbUrl;
                if (!mSite.isPrivate()) {
                    thumbUrl = PhotonUtils.getPhotonImageUrl(media.getUrl(), mThumbWidth, mThumbHeight);
                } else {
                    thumbUrl = UrlUtils.removeQuery(media.getUrl());
                }
                WordPressMediaUtils.loadNetworkImage(thumbUrl, holder.imageView, mImageLoader);
            }
        } else {
            // not an image, so show file name and file type
            holder.imageView.setImageDrawable(null);
            String fileName = media.getFileName();
            String title = media.getTitle();
            String fileExtension = MediaUtils.getExtensionForMimeType(media.getMimeType());
            holder.fileContainer.setVisibility(View.VISIBLE);
            holder.titleView.setText(TextUtils.isEmpty(title) ? fileName : title);
            holder.fileTypeView.setText(fileExtension.toUpperCase());
            int placeholderResId = WordPressMediaUtils.getPlaceholder(fileName);
            holder.fileTypeImageView.setImageResource(placeholderResId);
        }

        // show selection count when selected
        holder.selectionCountTextView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        if (isSelected) {
            int count = mSelectedItems.indexOf(media.getId()) + 1;
            holder.selectionCountTextView.setText(Integer.toString(count));
        }

        // make sure the thumbnail scale reflects its selection state
        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.imageView.getScaleX() != scale) {
            holder.imageView.setScaleX(scale);
            holder.imageView.setScaleY(scale);
        }

        // show upload state unless it's already uploaded
        if (state != MediaUploadState.UPLOADED) {
            holder.stateContainer.setVisibility(View.VISIBLE);

            // only show progress for items currently being uploaded or deleted
            boolean showProgress = state == MediaUploadState.UPLOADING || state == MediaUploadState.DELETING;
            holder.progressUpload.setVisibility(showProgress ? View.VISIBLE : View.GONE);

            // failed uploads can be retried
            if (state == MediaUploadState.FAILED) {
                holder.stateTextView.setText(mContext.getString(R.string.retry));
                holder.stateTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.media_retry_image, 0, 0);
            } else {
                holder.stateTextView.setText(MediaUploadState.getLabel(mContext, state));
                holder.stateTextView.setCompoundDrawables(null, null, null, null);
            }
        } else {
            holder.stateContainer.setVisibility(View.GONE);
            holder.stateContainer.setOnClickListener(null);
        }

        // if we are near the end, make a call to fetch more
        if (position == getItemCount() - 1
                && !mHasRetrievedAll
                && mCallback != null) {
            mCallback.onAdapterFetchMoreData();
        }
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final FadeInNetworkImageView imageView;
        private final TextView fileTypeView;
        private final ImageView fileTypeImageView;
        private final TextView selectionCountTextView;
        private final TextView stateTextView;
        private final ProgressBar progressUpload;
        private final ViewGroup stateContainer;
        private final ViewGroup fileContainer;
        private final ImageView imgPreview;

        public GridViewHolder(View view) {
            super(view);

            imageView = (FadeInNetworkImageView) view.findViewById(R.id.media_grid_item_image);
            selectionCountTextView = (TextView) view.findViewById(R.id.text_selection_count);

            stateContainer = (ViewGroup) view.findViewById(R.id.media_grid_item_upload_state_container);
            stateTextView = (TextView) stateContainer.findViewById(R.id.media_grid_item_upload_state);
            progressUpload = (ProgressBar) stateContainer.findViewById(R.id.media_grid_item_upload_progress);

            fileContainer = (ViewGroup) view.findViewById(R.id.media_grid_item_file_container);
            titleView = (TextView) fileContainer.findViewById(R.id.media_grid_item_name);
            fileTypeView = (TextView) fileContainer.findViewById(R.id.media_grid_item_filetype);
            fileTypeImageView = (ImageView) fileContainer.findViewById(R.id.media_grid_item_filetype_image);

            ViewGroup previewContainer = (ViewGroup) view.findViewById(R.id.frame_preview);
            previewContainer.setVisibility(mShowPreviewIcon ? View.VISIBLE : View.GONE);
            imgPreview = (ImageView) previewContainer.findViewById(R.id.image_preview);
            if (mShowPreviewIcon) {
                imgPreview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if (isValidPosition(position)) {
                            MediaModel media = mMediaList.get(position);
                            MediaPreviewActivity.showPreview(
                                    v.getContext(),
                                    imgPreview,
                                    mSite,
                                    media.getId());
                        }
                    }
                });
            }

            // make the progress bar white
            progressUpload.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

            // set size of image and container views
            imageView.getLayoutParams().width = mThumbWidth;
            imageView.getLayoutParams().height = mThumbHeight;
            stateContainer.getLayoutParams().width = mThumbWidth;
            stateContainer.getLayoutParams().height = mThumbHeight;
            fileContainer.getLayoutParams().width = mThumbWidth;
            fileContainer.getLayoutParams().height = mThumbHeight;

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (!isValidPosition(position)) {
                        return;
                    }
                    if (isInMultiSelect()) {
                        if (canSelectPosition(position)) {
                            toggleItemSelected(GridViewHolder.this, position);
                        }
                    } else if (mCallback != null) {
                        mCallback.onAdapterItemSelected(v, position);
                    }
                }
            });

            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (canSelectPosition(position)) {
                        if (isInMultiSelect()) {
                            toggleItemSelected(GridViewHolder.this, position);
                        } else if (mAllowMultiselect) {
                            setInMultiSelect(true);
                            setItemSelectedByPosition(GridViewHolder.this, position, true);
                        }
                    }
                    return true;
                }
            };
            itemView.setOnLongClickListener(longClickListener);
            stateTextView.setOnLongClickListener(longClickListener);

            stateTextView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (!isValidPosition(position)) {
                        return;
                    }
                    if (isInMultiSelect()) {
                        if (canSelectPosition(position)) {
                            toggleItemSelected(GridViewHolder.this, position);
                        }
                    } else {
                        // retry uploading this media item if it previously failed
                        MediaModel media = mMediaList.get(position);
                        MediaUploadState state = MediaUploadState.fromString(media.getUploadState());
                        if (state == MediaUploadState.FAILED) {
                            stateTextView.setText(R.string.media_upload_state_queued);
                            stateTextView.setCompoundDrawables(null, null, null, null);
                            if (mCallback != null) {
                                mCallback.onAdapterRetryUpload(media.getId());
                            }
                        } else if (mCallback != null) {
                            mCallback.onAdapterItemSelected(v, position);
                        }
                    }
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
            return mMediaList.get(position).getId();
        }
        return INVALID_POSITION;
    }

    /*
     * determines whether the media item at the passed position can be selected - not allowed
     * for deleted items since the whole purpose of multiselect is to delete multiple items
     */
    private boolean canSelectPosition(int position) {
        if (!mAllowMultiselect || !isValidPosition(position)) {
            return false;
        }
        MediaUploadState state = MediaUploadState.fromString(mMediaList.get(position).getUploadState());
        return state != MediaUploadState.DELETING && state != MediaUploadState.DELETED;
    }

    private void loadLocalImage(final String filePath, ImageView imageView) {
        imageView.setTag(filePath);

        Bitmap bitmap = WordPress.getBitmapCache().get(filePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageBitmap(null);
            try {
                new BitmapWorkerTask(imageView, mThumbWidth, mThumbHeight, new BitmapWorkerCallback() {
                    @Override
                    public void onBitmapReady(final String path, final ImageView imageView, final Bitmap bitmap) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                WordPress.getBitmapCache().put(path, bitmap);
                                if (imageView != null
                                        && imageView.getTag() instanceof String
                                        && ((String) imageView.getTag()).equalsIgnoreCase(path)) {
                                    imageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filePath);
            } catch (RejectedExecutionException e) {
                AppLog.e(AppLog.T.MEDIA, e);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
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
            holder.selectionCountTextView.setText(Integer.toString(mSelectedItems.indexOf(localMediaId) + 1));
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
        if (!isValidPosition(position)) {
            return;
        }
        int localMediaId = mMediaList.get(position).getId();
        boolean isSelected = mSelectedItems.contains(localMediaId);
        setItemSelectedByPosition(holder, position, !isSelected);
    }

    public void setSelectedItems(ArrayList<Integer> selectedItems) {
        mSelectedItems.clear();
        mSelectedItems.addAll(selectedItems);
        if (mCallback != null) {
            mCallback.onAdapterSelectionCountChanged(mSelectedItems.size());
        }
        notifyDataSetChanged();
    }

    /*
     * returns true if the passed list is the same as the existing one
     */
    private boolean isSameList(@NonNull List<MediaModel> mediaList) {
        if (mediaList.size() != mMediaList.size()) {
            return false;
        }

        for (MediaModel media: mediaList) {
            MediaModel thisMedia = getMediaFromId(media.getId());
            if (thisMedia == null || !thisMedia.equals(media)) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns the media item with the passed media ID in the current media list
     */
    private MediaModel getMediaFromId(int id) {
        for (int i = 0; i < mMediaList.size(); i++) {
            if (mMediaList.get(i).getId() == id) {
                return mMediaList.get(i);
            }
        }
        return null;
    }
}
