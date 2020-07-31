package org.wordpress.android.ui.photopicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.Callback;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PhotoPickerUtils;
import org.wordpress.android.util.ViewUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ThumbnailViewHolder> {
    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;

    private static final AniUtils.Duration ANI_DURATION = AniUtils.Duration.SHORT;

    private final Context mContext;

    private boolean mLoadThumbnails = true;

    private final LayoutInflater mInflater;

    private final ArrayList<PhotoPickerUiItem> mMediaList = new ArrayList<>();

    protected final ImageManager mImageManager;

    PhotoPickerAdapter(Context context,
                       ImageManager imageManager) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mImageManager = imageManager;

        setHasStableIds(true);
    }

    void loadData(List<PhotoPickerUiItem> result) {
        DiffResult diffResult = DiffUtil.calculateDiff(new Callback() {
            @Override public int getOldListSize() {
                return mMediaList.size();
            }

            @Override public int getNewListSize() {
                return result.size();
            }

            @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mMediaList.get(oldItemPosition).getId() == result.get(newItemPosition).getId();
            }

            @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mMediaList.get(oldItemPosition).equals(result.get(newItemPosition));
            }

            @Nullable @Override public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                PhotoPickerUiItem updatedItem = result.get(newItemPosition);
                if (mMediaList.get(oldItemPosition).isSelected() != updatedItem.isSelected()) {
                    return updatedItem.getId();
                }
                return super.getChangePayload(oldItemPosition, newItemPosition);
            }
        });
        mMediaList.clear();
        mMediaList.addAll(result);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    @Override
    public long getItemId(int position) {
        if (isValidPosition(position)) {
            return getItemAtPosition(position).getId();
        } else {
            return NO_POSITION;
        }
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

    @NonNull @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_picker_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    private void updateSelectionCountForPosition(PhotoPickerUiItem item, @NonNull TextView txtSelectionCount) {
        if (item.getSelectedOrder() != null) {
            txtSelectionCount.setText(String.format(Locale.getDefault(), "%d", item.getSelectedOrder()));
        } else {
            txtSelectionCount.setText(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position, @NonNull List<Object> payloads) {
        PhotoPickerUiItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }
        boolean animateSelection = false;
        for (Object payload : payloads) {
            if (payload instanceof Long && payload.equals(item.getId())) {
                animateSelection = true;
            }
        }
        holder.bind(item, animateSelection);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        PhotoPickerUiItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }
        holder.bind(item, false);
    }

    private PhotoPickerUiItem getItemAtPosition(int position) {
        if (!isValidPosition(position)) {
            AppLog.w(AppLog.T.POSTS, "photo picker > invalid position in getItemAtPosition");
            return null;
        }
        return mMediaList.get(position);
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < mMediaList.size();
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

            ViewUtils.addCircularShadowOutline(mTxtSelectionCount);
        }

        private void addImageSelectedToAccessibilityFocusedEvent(ImageView imageView, PhotoPickerUiItem item) {
            AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(imageView, event -> {
                int position = getAdapterPosition();
                if (isValidPosition(position)) {
                    String imageSelectedText = imageView.getContext()
                                                        .getString(R.string.photo_picker_image_selected);

                    if (item.isSelected()) {
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

        public void bind(PhotoPickerUiItem item, boolean animateSelection) {
            boolean isSelected = item.isSelected();
            mTxtSelectionCount.setSelected(isSelected);
            updateSelectionCountForPosition(item, mTxtSelectionCount);

            if (item.getShowOrderMarker()) {
                mTxtSelectionCount.setBackgroundResource(R.drawable.photo_picker_circle_pressed);
            }

            mVideoOverlay.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

            if (mLoadThumbnails) {
                mImageManager.load(mImgThumbnail, ImageType.PHOTO, item.getUri().toString(), ScaleType.FIT_CENTER);
            } else {
                mImageManager.cancelRequestAndClearImageView(mImgThumbnail);
            }

            addImageSelectedToAccessibilityFocusedEvent(mImgThumbnail, item);
            mImgThumbnail.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (isValidPosition(position)) {
                    item.getToggleAction().toggle();
                    PhotoPickerUtils.announceSelectedImageForAccessibility(mImgThumbnail, item.isSelected());
                }
            });

            mImgThumbnail.setOnLongClickListener(v -> {
                showPreview(item);
                return true;
            });
            ViewUtilsKt.redirectContextClickToLongPressListener(mImgThumbnail);

            mVideoOverlay.setOnClickListener(v -> {
                showPreview(item);
            });

            if (animateSelection) {
                if (isSelected) {
                    AniUtils.scale(mImgThumbnail, SCALE_NORMAL, SCALE_SELECTED, ANI_DURATION);
                } else {
                    AniUtils.scale(mImgThumbnail, SCALE_SELECTED, SCALE_NORMAL, ANI_DURATION);
                }

                if (item.getShowOrderCounter()) {
                    AniUtils.startAnimation(mTxtSelectionCount, R.anim.pop);
                } else if (isSelected) {
                    AniUtils.fadeIn(mTxtSelectionCount, ANI_DURATION);
                } else {
                    AniUtils.fadeOut(mTxtSelectionCount, ANI_DURATION);
                }
            } else {
                float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
                if (mImgThumbnail.getScaleX() != scale) {
                    mImgThumbnail.setScaleX(scale);
                    mImgThumbnail.setScaleY(scale);
                }
                mTxtSelectionCount.setVisibility(item.getShowOrderCounter() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void showPreview(PhotoPickerUiItem item) {
        if (item != null) {
            trackOpenPreviewScreenEvent(item);
            MediaPreviewActivity.showPreview(
                    mContext,
                    null,
                    item.getUri().toString());
        }
    }

    private void trackOpenPreviewScreenEvent(final PhotoPickerUiItem item) {
        if (item == null) {
            return;
        }

        new Thread(() -> {
            Map<String, Object> properties =
                    AnalyticsUtils.getMediaProperties(mContext, item.isVideo(), item.getUri(), null);
            properties.put("is_video", item.isVideo());
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED, properties);
        }).start();
    }
}
