package org.wordpress.android.ui.photopicker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.ui.photopicker.PhotoPickerAdapterDiffCallback.Payload;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PhotoPickerUtils;
import org.wordpress.android.util.ViewUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ThumbnailViewHolder> {
    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;

    private static final AniUtils.Duration ANI_DURATION = AniUtils.Duration.SHORT;

    private boolean mLoadThumbnails = true;

    private List<PhotoPickerUiItem> mMediaList = new ArrayList<>();

    protected final ImageManager mImageManager;

    PhotoPickerAdapter(ImageManager imageManager) {
        super();
        mImageManager = imageManager;

        setHasStableIds(true);
    }

    void loadData(List<PhotoPickerUiItem> result) {
        DiffResult diffResult = DiffUtil.calculateDiff(new PhotoPickerAdapterDiffCallback(mMediaList, result));
        mMediaList = result;
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    @Override
    public long getItemId(int position) {
        return mMediaList.get(position).getId();
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_picker_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position, @NonNull List<Object> payloads) {
        PhotoPickerUiItem item = mMediaList.get(position);
        boolean animateSelection = false;
        boolean updateCount = false;
        for (Object payload : payloads) {
            if (payload == Payload.SELECTION_CHANGE) {
                animateSelection = true;
            }
            if (payload == Payload.COUNT_CHANGE) {
                updateCount = true;
            }
        }
        holder.bind(item, animateSelection, updateCount);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
        holder.bind(mMediaList.get(position), false, false);
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
            });
        }

        public void bind(PhotoPickerUiItem item, boolean animateSelection, boolean updateCount) {
            // Only count is updated so do not redraw the whole item
            if (updateCount) {
                updateSelectionCountForPosition(item, mTxtSelectionCount);
                AniUtils.startAnimation(mTxtSelectionCount, R.anim.pop);
                return;
            }
            boolean isSelected = item.isSelected();
            mTxtSelectionCount.setSelected(isSelected);
            updateSelectionCountForPosition(item, mTxtSelectionCount);

            mVideoOverlay.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

            if (!item.getShowOrderCounter()) {
                mTxtSelectionCount.setBackgroundResource(R.drawable.photo_picker_circle_pressed);
            }

            if (mLoadThumbnails) {
                mImageManager.load(mImgThumbnail, ImageType.PHOTO, item.getUri().toString(), ScaleType.FIT_CENTER);
            } else {
                mImageManager.cancelRequestAndClearImageView(mImgThumbnail);
            }

            addImageSelectedToAccessibilityFocusedEvent(mImgThumbnail, item);
            mImgThumbnail.setOnClickListener(v -> {
                item.getToggleAction().toggle();
                PhotoPickerUtils.announceSelectedImageForAccessibility(mImgThumbnail, item.isSelected());
            });

            mImgThumbnail.setOnLongClickListener(v -> {
                item.getClickAction().click();
                return true;
            });
            ViewUtilsKt.redirectContextClickToLongPressListener(mImgThumbnail);

            mVideoOverlay.setOnClickListener(v -> item.getClickAction().click());

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
                mTxtSelectionCount.setVisibility(item.getShowOrderCounter() || isSelected ? View.VISIBLE : View.GONE);
            }
        }

        private void updateSelectionCountForPosition(PhotoPickerUiItem item, @NonNull TextView txtSelectionCount) {
            if (item.getSelectedOrder() != null) {
                txtSelectionCount.setText(String.format(Locale.getDefault(), "%d", item.getSelectedOrder()));
            } else {
                txtSelectionCount.setText(null);
            }
        }
    }
}
