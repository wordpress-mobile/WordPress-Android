package org.wordpress.android.ui.notifications.blocks;

import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.fluxc.tools.FormattableMedia;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block of data displayed in a notification.
 * This basic block can support a media item (image/video) and/or text.
 */
public class NoteBlock {
    private final JSONObject mNoteData; // TODO remove when NoteBlock's children are refactored to FormattableContent
    private final FormattableContent mNoteData2;
    private final OnNoteBlockTextClickListener mOnNoteBlockTextClickListener;
    protected final ImageManager mImageManager;
    protected final NotificationsUtilsWrapper mNotificationsUtilsWrapper;
    private boolean mIsBadge;
    private boolean mIsPingback;
    private boolean mHasAnimatedBadge;
    private int mBackgroundColor;

    public interface OnNoteBlockTextClickListener {
        void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan);

        void showDetailForNoteIds();

        void showReaderPostComments();

        void showSitePreview(long siteId, String siteUrl);
    }

    public NoteBlock(JSONObject noteObject, ImageManager imageManager,
                     NotificationsUtilsWrapper notificationsUtilsWrapper,
                     OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        mNoteData = noteObject;
        mNoteData2 = notificationsUtilsWrapper.mapJsonToFormattablbeContent(noteObject);
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

    JSONObject getNoteData() {
        return mNoteData;
    }

    FormattableContent getNoteData2() {
        return mNoteData2;
    }

    Spannable getNoteText() {
        return mNotificationsUtilsWrapper.getSpannableContentForRanges(mNoteData2, null,
                mOnNoteBlockTextClickListener, false);
    }

    String getMetaHomeTitle() {
        if (mNoteData2.getMeta() != null && mNoteData2.getMeta().getTitles() != null) {
            return StringUtils.notNullStr(mNoteData2.getMeta().getTitles().getHome());
        }
        return "";
    }

    long getMetaSiteId() {
        if (mNoteData2.getMeta() != null && mNoteData2.getMeta().getIds() != null) {
            Long siteId = mNoteData2.getMeta().getIds().getSite();
            return siteId == null ? -1 : siteId;
        }
        return -1;
    }

    public String getMetaSiteUrl() {
        if (mNoteData2.getMeta() != null && mNoteData2.getMeta().getLinks() != null) {
            return StringUtils.notNullStr(mNoteData2.getMeta().getLinks().getHome());
        }
        return "";
    }

    private boolean isPingBack() {
        return mIsPingback;
    }

    public void setIsPingback() {
        mIsPingback = true;
    }

    FormattableMedia getNoteMediaItem() {
        return (mNoteData2.getMedia() != null && mNoteData2.getMedia().size() > 0) ? mNoteData2.getMedia().get(0)
                : null;
    }

    public void setIsBadge() {
        mIsBadge = true;
    }

    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    public int getLayoutResourceId() {
        return R.layout.note_block_basic;
    }

    private boolean hasMediaArray() {
        return mNoteData2.getMedia() != null && !mNoteData2.getMedia().isEmpty();
    }

    boolean hasImageMediaItem() {
        return hasMediaArray()
               && (getNoteMediaItem().getType().startsWith("image") || getNoteMediaItem().getType().equals("badge"))
               && !TextUtils.isEmpty(getNoteMediaItem().getUrl());
    }

    private boolean hasVideoMediaItem() {
        return hasMediaArray()
               && !TextUtils.isEmpty(getNoteMediaItem().getType())
               && getNoteMediaItem().getType().startsWith("video")
               && !TextUtils.isEmpty(getNoteMediaItem().getUrl());
    }

    public boolean containsBadgeMediaType() {
        if (mNoteData2.getMedia() != null) {
            for (FormattableMedia mediaObject : mNoteData2.getMedia()) {
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
            mImageManager
                    .loadWithResultListener(noteBlockHolder.getImageView(), ImageType.IMAGE,
                            StringUtils.notNullStr(getNoteMediaItem().getUrl()), null,
                            new ImageManager.RequestListener<Drawable>() {
                                @Override
                                public void onLoadFailed(@Nullable Exception e) {
                                    if (e != null) {
                                        AppLog.e(T.NOTIFS, e);
                                    }
                                    noteBlockHolder.hideImageView();
                                }

                                @Override
                                public void onResourceReady(@Nullable Drawable resource) {
                                    if (!mHasAnimatedBadge && view.getContext() != null && resource != null) {
                                        mHasAnimatedBadge = true;
                                        Animation pop = AnimationUtils.loadAnimation(view.getContext(), R.anim.pop);
                                        noteBlockHolder.getImageView().startAnimation(pop);
                                    }
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
                noteBlockHolder.getDivider().setVisibility(View.VISIBLE);
                noteBlockHolder.getButton().setVisibility(View.VISIBLE);
                noteBlockHolder.getButton().setText(noteText.toString());
                noteBlockHolder.getButton().setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getOnNoteBlockTextClickListener() != null) {
                            getOnNoteBlockTextClickListener().showSitePreview(0, getMetaSiteUrl());
                        }
                    }
                });
            } else {
                if (mIsBadge) {
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT);
                    params.gravity = Gravity.CENTER_HORIZONTAL;
                    noteBlockHolder.getTextView().setLayoutParams(params);
                    noteBlockHolder.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
                    noteBlockHolder.getTextView().setPadding(0, DisplayUtils.dpToPx(view.getContext(), 8), 0, 0);

                    if (AccessibilityUtils.isAccessibilityEnabled(noteBlockHolder.getTextView().getContext())) {
                        noteBlockHolder.getTextView().setClickable(false);
                        noteBlockHolder.getTextView().setLongClickable(false);
                    }
                } else {
                    noteBlockHolder.getTextView().setGravity(Gravity.NO_GRAVITY);
                    noteBlockHolder.getTextView().setPadding(0, 0, 0, 0);
                }
                noteBlockHolder.getTextView().setText(noteText);
                noteBlockHolder.getTextView().setVisibility(View.VISIBLE);
            }
        } else {
            noteBlockHolder.getTextView().setVisibility(View.GONE);
        }


        view.setBackgroundColor(mBackgroundColor);

        return view;
    }

    public Object getViewHolder(View view) {
        return new BasicNoteBlockHolder(view);
    }

    static class BasicNoteBlockHolder {
        private final LinearLayout mRootLayout;
        private final WPTextView mTextView;
        private final Button mButton;
        private final View mDivider;

        private ImageView mImageView;
        private VideoView mVideoView;

        BasicNoteBlockHolder(View view) {
            mRootLayout = (LinearLayout) view;
            mTextView = view.findViewById(R.id.note_text);
            mTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            mButton = view.findViewById(R.id.note_button);
            mDivider = view.findViewById(R.id.divider_view);
        }

        public WPTextView getTextView() {
            return mTextView;
        }

        public Button getButton() {
            return mButton;
        }

        public View getDivider() {
            return mDivider;
        }

        public ImageView getImageView() {
            if (mImageView == null) {
                mImageView = new ImageView(mRootLayout.getContext());
                int imageSize = DisplayUtils.dpToPx(mRootLayout.getContext(), 180);
                int imagePadding = mRootLayout.getContext().getResources().getDimensionPixelSize(R.dimen.margin_large);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageSize, imageSize);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                mImageView.setLayoutParams(layoutParams);
                mImageView.setPadding(0, imagePadding, 0, 0);
                mRootLayout.addView(mImageView, 0);
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
