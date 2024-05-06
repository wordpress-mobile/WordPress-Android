package org.wordpress.android.ui.notifications.blocks;

import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.fluxc.tools.FormattableMedia;
import org.wordpress.android.fluxc.tools.FormattableRange;
import org.wordpress.android.util.image.GlidePopTransitionOptions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormattableContentUtilsKt;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block of data displayed in a notification.
 * This basic block can support a media item (image/video) and/or text.
 */
public class NoteBlock {
    private final FormattableContent mNoteData;
    private final OnNoteBlockTextClickListener mOnNoteBlockTextClickListener;
    protected final ImageManager mImageManager;
    protected final NotificationsUtilsWrapper mNotificationsUtilsWrapper;
    private boolean mIsBadge;
    private boolean mIsPingback;
    private boolean mIsViewMilestone;

    public interface OnNoteBlockTextClickListener {
        void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan);

        void showDetailForNoteIds();

        void showReaderPostComments();

        void showSitePreview(long siteId, String siteUrl);
    }

    public NoteBlock(FormattableContent noteObject, ImageManager imageManager,
                     NotificationsUtilsWrapper notificationsUtilsWrapper,
                     OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        mNoteData = noteObject;
        mOnNoteBlockTextClickListener = onNoteBlockTextClickListener;
        mImageManager = imageManager;
        mNotificationsUtilsWrapper = notificationsUtilsWrapper;
    }

    OnNoteBlockTextClickListener getOnNoteBlockTextClickListener() {
        return mOnNoteBlockTextClickListener;
    }

    public BlockType getBlockType() {
        return BlockType.BASIC;
    }

    FormattableContent getNoteData() {
        return mNoteData;
    }

    Spannable getNoteText() {
        return mNotificationsUtilsWrapper.getSpannableContentForRanges(mNoteData, null,
                mOnNoteBlockTextClickListener, false);
    }

    String getMetaHomeTitle() {
        return FormattableContentUtilsKt.getMetaTitlesHomeOrEmpty(mNoteData);
    }

    long getMetaSiteId() {
        return FormattableContentUtilsKt.getMetaIdsSiteIdOrZero(mNoteData);
    }

    public String getMetaSiteUrl() {
        return FormattableContentUtilsKt.getMetaLinksHomeOrEmpty(mNoteData);
    }

    private boolean isPingBack() {
        return mIsPingback;
    }

    public void setIsPingback() {
        mIsPingback = true;
    }

    @Nullable public FormattableMedia getNoteMediaItem() {
        return FormattableContentUtilsKt.getMediaOrNull(mNoteData, 0);
    }

    public void setIsBadge() {
        mIsBadge = true;
    }

    public void setIsViewMilestone() {
        mIsViewMilestone = true;
    }

    public int getLayoutResourceId() {
        return R.layout.note_block_basic;
    }

    private boolean hasMediaArray() {
        return mNoteData.getMedia() != null && !mNoteData.getMedia().isEmpty();
    }

    public boolean hasImageMediaItem() {
        return hasMediaArray()
               && getNoteMediaItem() != null
               && !TextUtils.isEmpty(getNoteMediaItem().getType())
               && (getNoteMediaItem().getType().startsWith("image") || getNoteMediaItem().getType().equals("badge"))
               && !TextUtils.isEmpty(getNoteMediaItem().getUrl());
    }

    private boolean hasVideoMediaItem() {
        return hasMediaArray()
               && getNoteMediaItem() != null
               && !TextUtils.isEmpty(getNoteMediaItem().getType())
               && getNoteMediaItem().getType().startsWith("video")
               && !TextUtils.isEmpty(getNoteMediaItem().getUrl());
    }

    public boolean containsBadgeMediaType() {
        if (mNoteData.getMedia() != null) {
            for (FormattableMedia mediaObject : mNoteData.getMedia()) {
                if ("badge".equals(mediaObject.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public View configureView(final View view) {
        final BasicNoteBlockHolder noteBlockHolder = (BasicNoteBlockHolder) view.getTag();

        // Note image
        if (hasImageMediaItem()) {
            noteBlockHolder.getImageView().setVisibility(View.VISIBLE);
            // Request image, and animate it when loaded
            mImageManager.animateWithResultListener(noteBlockHolder.getImageView(), ImageType.IMAGE,
                    StringUtils.notNullStr(getNoteMediaItem().getUrl()),
                    GlidePopTransitionOptions.INSTANCE.pop(),
                    new ImageManager.RequestListener<Drawable>() {
                        @Override
                        public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                            if (e != null) {
                                AppLog.e(T.NOTIFS, e);
                            }
                            noteBlockHolder.hideImageView();
                        }

                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                        }
                    });

            if (mIsBadge) {
                noteBlockHolder.getImageView().setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
        } else {
            mImageManager.cancelRequestAndClearImageView(noteBlockHolder.getImageView());
            noteBlockHolder.hideImageView();
        }

        // Note video
        if (hasVideoMediaItem()) {
            noteBlockHolder.getVideoView().setVideoURI(Uri.parse(StringUtils.notNullStr(getNoteMediaItem().getUrl())));
            noteBlockHolder.getVideoView().setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.hideVideoView();
        }

        // Note text
        Spannable noteText = getNoteText();
        if (!TextUtils.isEmpty(noteText)) {
            if (isPingBack()) {
                noteBlockHolder.getTextView().setVisibility(View.GONE);
                noteBlockHolder.getMaterialButton().setVisibility(View.GONE);
                noteBlockHolder.getDivider().setVisibility(View.VISIBLE);
                noteBlockHolder.getButton().setVisibility(View.VISIBLE);
                noteBlockHolder.getButton().setText(noteText.toString());
                noteBlockHolder.getButton().setOnClickListener(v -> {
                    if (getOnNoteBlockTextClickListener() != null) {
                        getOnNoteBlockTextClickListener().showSitePreview(0, getMetaSiteUrl());
                    }
                });
            } else {
                int textViewVisibility = View.VISIBLE;
                if (mIsBadge) {
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT);
                    params.gravity = Gravity.CENTER_HORIZONTAL;
                    noteBlockHolder.getTextView().setLayoutParams(params);
                    noteBlockHolder.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
                    int padding;
                    if (mIsViewMilestone) {
                        padding = 40;
                    } else {
                        padding = 8;
                    }
                    noteBlockHolder.getTextView().setPadding(0, DisplayUtils.dpToPx(view.getContext(), padding), 0, 0);

                    if (AccessibilityUtils.isAccessibilityEnabled(noteBlockHolder.getTextView().getContext())) {
                        noteBlockHolder.getTextView().setClickable(false);
                        noteBlockHolder.getTextView().setLongClickable(false);
                    }
                    if (mIsViewMilestone) {
                        if (FormattableContentUtilsKt.isMobileButton(mNoteData)) {
                            textViewVisibility = View.GONE;
                            noteBlockHolder.getButton().setVisibility(View.GONE);
                            noteBlockHolder.getMaterialButton().setVisibility(View.VISIBLE);
                            noteBlockHolder.getMaterialButton().setText(noteText.toString());
                            noteBlockHolder.getMaterialButton().setOnClickListener(v -> {
                                FormattableRange buttonRange =
                                        FormattableContentUtilsKt.getMobileButtonRange(mNoteData);
                                if (getOnNoteBlockTextClickListener() != null && buttonRange != null) {
                                    NoteBlockClickableSpan clickableSpan =
                                            new NoteBlockClickableSpan(buttonRange, true, false);
                                    getOnNoteBlockTextClickListener().onNoteBlockTextClicked(clickableSpan);
                                }
                            });
                        } else {
                            noteBlockHolder.getTextView().setTextSize(28);
                            TypefaceSpan typefaceSpan = new TypefaceSpan("sans-serif");
                            noteText.setSpan(typefaceSpan, 0, noteText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                } else {
                    noteBlockHolder.getTextView().setGravity(Gravity.NO_GRAVITY);
                    noteBlockHolder.getTextView().setPadding(0, 0, 0, 0);
                }

                NoteBlockClickableSpan[] spans = noteText.getSpans(0, noteText.length(), NoteBlockClickableSpan.class);
                for (NoteBlockClickableSpan span : spans) {
                    span.enableColors(view.getContext());
                }
                noteBlockHolder.getTextView().setText(noteText);
                noteBlockHolder.getTextView().setVisibility(textViewVisibility);
            }
        } else {
            noteBlockHolder.getButton().setVisibility(View.GONE);
            noteBlockHolder.getDivider().setVisibility(View.GONE);
            noteBlockHolder.getMaterialButton().setVisibility(View.GONE);
            noteBlockHolder.getTextView().setVisibility(View.GONE);
        }

        return view;
    }

    public Object getViewHolder(View view) {
        return new BasicNoteBlockHolder(view);
    }

    static class BasicNoteBlockHolder {
        private final LinearLayout mRootLayout;
        private final WPTextView mTextView;
        private final Button mButton;
        private final Button mMaterialButton;
        private final View mDivider;

        private ImageView mImageView;
        private VideoView mVideoView;

        BasicNoteBlockHolder(View view) {
            mRootLayout = (LinearLayout) view;
            mTextView = view.findViewById(R.id.note_text);
            mTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            mButton = view.findViewById(R.id.note_button);
            mMaterialButton = view.findViewById(R.id.note_material_button);
            mDivider = view.findViewById(R.id.divider_view);
        }

        public WPTextView getTextView() {
            return mTextView;
        }

        public Button getButton() {
            return mButton;
        }

        public Button getMaterialButton() {
            return mMaterialButton;
        }

        public View getDivider() {
            return mDivider;
        }

        public ImageView getImageView() {
            if (mImageView == null) {
                mImageView = mRootLayout.findViewById(R.id.image);
            }

            return mImageView;
        }

        public VideoView getVideoView() {
            if (mVideoView == null) {
                mVideoView = new VideoView(mRootLayout.getContext());
                FrameLayout.LayoutParams layoutParams =
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                DisplayUtils.dpToPx(mRootLayout.getContext(), 220));
                mVideoView.setLayoutParams(layoutParams);
                mRootLayout.addView(mVideoView, 0);

                // Attach a mediaController if we are displaying a video.
                final MediaController mediaController = new MediaController(mRootLayout.getContext());
                mediaController.setMediaPlayer(mVideoView);

                mVideoView.setMediaController(mediaController);
                mediaController.requestFocus();
                mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        // Show the media controls when the video is ready to be played.
                        mediaController.show(0);
                    }
                });
            }

            return mVideoView;
        }

        public void hideImageView() {
            if (mImageView != null) {
                mImageView.setVisibility(View.GONE);
            }
        }

        public void hideVideoView() {
            if (mVideoView != null) {
                mVideoView.setVisibility(View.GONE);
            }
        }
    }
}
