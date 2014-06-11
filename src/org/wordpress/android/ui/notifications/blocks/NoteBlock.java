package org.wordpress.android.ui.notifications.blocks;

import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.NotificationUtils;
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

    public BlockType getBlockType() {
        return BlockType.BASIC;
    }

    public JSONObject getNoteData() {
        return mNoteData;
    }

    public Spannable getNoteText() {
        return NotificationUtils.getSpannableTextFromIndices(mNoteData, true, mOnNoteBlockTextClickListener);
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
            noteBlockHolder.mImageView.setImageUrl(mMediaItem.optString("url", ""), WordPress.imageLoader);
            noteBlockHolder.mImageView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.mImageView.setVisibility(View.GONE);
        }

        // Note video
        if (hasVideoMediaItem()) {
            noteBlockHolder.mVideoView.setVideoURI(Uri.parse(mMediaItem.optString("url", "")));
            noteBlockHolder.mVideoView.setVisibility(View.VISIBLE);

            // Attach a mediaController if we are displaying a video.
            final MediaController mediaController = new MediaController(noteBlockHolder.mVideoView.getContext());
            mediaController.setMediaPlayer(noteBlockHolder.mVideoView);

            noteBlockHolder.mVideoView.setMediaController(mediaController);
            mediaController.requestFocus();
            noteBlockHolder.mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Show the media controls when the video is ready to be played.
                    mediaController.show(0);
                }
            });
        } else {
            noteBlockHolder.mVideoView.setVisibility(View.GONE);
        }

        // Note text
        if (!TextUtils.isEmpty(getNoteText())) {
            noteBlockHolder.mTextView.setText(getNoteText());
            noteBlockHolder.mTextView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.mTextView.setVisibility(View.GONE);
        }

        return view;
    }

    public Object getViewHolder(View view) {
        return new BasicNoteBlockHolder(view);
    }

    private static class BasicNoteBlockHolder {
        private final WPTextView mTextView;
        private final NetworkImageView mImageView;
        private final VideoView mVideoView;

        BasicNoteBlockHolder(View view) {
            mTextView = (WPTextView) view.findViewById(R.id.note_text);
            mTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            mImageView = (NetworkImageView) view.findViewById(R.id.note_image);
            mVideoView = (VideoView) view.findViewById(R.id.note_video);
        }
    }
}
