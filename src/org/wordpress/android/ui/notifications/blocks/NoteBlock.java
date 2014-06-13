package org.wordpress.android.ui.notifications.blocks;

import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block of data displayed in a notification.
 * This basic block can support a media item (image/video) and/or text.
 */
public class NoteBlock {

    private static String PROPERTY_MEDIA_TYPE = "type";
    private static String PROPERTY_MEDIA_URL = "url";

    private JSONObject mNoteData;
    private OnNoteBlockTextClickListener mOnNoteBlockTextClickListener;
    private JSONObject mMediaItem;

    public interface OnNoteBlockTextClickListener {
        public void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan);
    }

    public NoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        mNoteData = noteObject;
        mOnNoteBlockTextClickListener = onNoteBlockTextClickListener;
    }

    public OnNoteBlockTextClickListener getOnNoteBlockTextClickListener() {
        return mOnNoteBlockTextClickListener;
    }

    public BlockType getBlockType() {
        return BlockType.BASIC;
    }

    public JSONObject getNoteData() {
        return mNoteData;
    }

    public Spannable getNoteText() {
        return NotificationUtils.getSpannableTextFromIndices(mNoteData, mOnNoteBlockTextClickListener);
    }

    public JSONObject getNoteMediaItem() {
        if (mMediaItem == null) {
            mMediaItem = JSONUtil.queryJSON(mNoteData, "media[0]", new JSONObject());
        }

        return mMediaItem;
    }

    public int getLayoutResourceId() {
        return R.layout.note_block_basic;
    }

    private boolean hasMediaArray() {
        return mNoteData.has("media");
    }

    public boolean hasImageMediaItem() {
        return hasMediaArray() &&
                getNoteMediaItem().optString(PROPERTY_MEDIA_TYPE, "").startsWith("image") &&
                getNoteMediaItem().has(PROPERTY_MEDIA_URL);
    }

    public boolean hasVideoMediaItem() {
        return hasMediaArray() &&
                getNoteMediaItem().optString(PROPERTY_MEDIA_TYPE, "").startsWith("video") &&
                getNoteMediaItem().has(PROPERTY_MEDIA_URL);
    }

    public View configureView(View view) {
        final BasicNoteBlockHolder noteBlockHolder = (BasicNoteBlockHolder)view.getTag();

        // Note image
        if (hasImageMediaItem()) {
            noteBlockHolder.getImageView().setImageUrl(mMediaItem.optString("url", ""), WordPress.imageLoader);
            noteBlockHolder.getImageView().setVisibility(View.VISIBLE);
        } else if (noteBlockHolder.imageView != null) {
            noteBlockHolder.getImageView().setVisibility(View.GONE);
        }

        // Note video
        if (hasVideoMediaItem()) {
            noteBlockHolder.getVideoView().setVideoURI(Uri.parse(mMediaItem.optString("url", "")));
            noteBlockHolder.getVideoView().setVisibility(View.VISIBLE);

            // Attach a mediaController if we are displaying a video.
            final MediaController mediaController = new MediaController(noteBlockHolder.getVideoView().getContext());
            mediaController.setMediaPlayer(noteBlockHolder.getVideoView());

            noteBlockHolder.getVideoView().setMediaController(mediaController);
            mediaController.requestFocus();
            noteBlockHolder.getVideoView().setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Show the media controls when the video is ready to be played.
                    mediaController.show(0);
                }
            });
        } else if (noteBlockHolder.videoView != null) {
            noteBlockHolder.getVideoView().setVisibility(View.GONE);
        }

        // Note text
        if (!TextUtils.isEmpty(getNoteText())) {
            noteBlockHolder.getTextView().setText(getNoteText());
            noteBlockHolder.getTextView().setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.getTextView().setVisibility(View.GONE);
        }

        return view;
    }

    public Object getViewHolder(View view) {
        return new BasicNoteBlockHolder(view);
    }

    private static class BasicNoteBlockHolder {
        public NetworkImageView imageView;
        public VideoView videoView;

        private final LinearLayout mRootLayout;
        private final WPTextView mTextView;

        BasicNoteBlockHolder(View view) {
            mRootLayout = (LinearLayout)view;
            mTextView = (WPTextView) view.findViewById(R.id.note_text);
            mTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
        }

        public WPTextView getTextView() {
            return mTextView;
        }

        public NetworkImageView getImageView() {
            if (imageView == null) {
                imageView = new NetworkImageView(mRootLayout.getContext());
                int imageSize = DisplayUtils.dpToPx(mRootLayout.getContext(), 220);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageSize, imageSize);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                layoutParams.setMargins(0, 0, 0, DisplayUtils.dpToPx(mRootLayout.getContext(), 16));
                imageView.setLayoutParams(layoutParams);
                mRootLayout.addView(imageView);
            }

            return imageView;
        }

        public VideoView getVideoView() {
            if (videoView == null) {
                videoView = new VideoView(mRootLayout.getContext());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        DisplayUtils.dpToPx(mRootLayout.getContext(), 220));
                layoutParams.setMargins(0, 0, 0, DisplayUtils.dpToPx(mRootLayout.getContext(), 16));
                videoView.setLayoutParams(layoutParams);
                mRootLayout.addView(videoView);
            }

            return videoView;
        }
    }
}
