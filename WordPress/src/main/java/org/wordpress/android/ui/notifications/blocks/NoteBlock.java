package org.wordpress.android.ui.notifications.blocks;

import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block of data displayed in a notification.
 * This basic block can support a media item (image/video) and/or text.
 */
public class NoteBlock {

    private static final String PROPERTY_MEDIA_TYPE = "type";
    private static final String PROPERTY_MEDIA_URL = "url";

    private final JSONObject mNoteData;
    private final OnNoteBlockTextClickListener mOnNoteBlockTextClickListener;
    private JSONObject mMediaItem;
    private boolean mIsBadge;
    private boolean mHasAnimatedBadge;
    private int mBackgroundColor;

    public interface OnNoteBlockTextClickListener {
        void onNoteBlockTextClicked(NoteBlockClickableSpan clickedSpan);
        void showDetailForNoteIds();
        void showReaderPostComments();
        void showSitePreview(long siteId, String siteUrl);
    }

    public NoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        mNoteData = noteObject;
        mOnNoteBlockTextClickListener = onNoteBlockTextClickListener;
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

    public Spannable getNoteText() {
        return NotificationsUtils.getSpannableContentForRanges(mNoteData, null,
                mOnNoteBlockTextClickListener, false);
    }

    public String getMetaHomeTitle() {
        return JSONUtils.queryJSON(mNoteData, "meta.titles.home", "");
    }

    public long getMetaSiteId() {
        return JSONUtils.queryJSON(mNoteData, "meta.ids.site", -1);
    }

    public String getMetaSiteUrl() {
        return JSONUtils.queryJSON(mNoteData, "meta.links.home", "");
    }

    JSONObject getNoteMediaItem() {
        if (mMediaItem == null) {
            mMediaItem = JSONUtils.queryJSON(mNoteData, "media[0]", new JSONObject());
        }

        return mMediaItem;
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
        return mNoteData.has("media");
    }

    boolean hasImageMediaItem() {
        String mediaType = getNoteMediaItem().optString(PROPERTY_MEDIA_TYPE, "");
        return hasMediaArray() &&
                (mediaType.startsWith("image") || mediaType.equals("badge")) &&
                getNoteMediaItem().has(PROPERTY_MEDIA_URL);
    }

    boolean hasVideoMediaItem() {
        return hasMediaArray() &&
                getNoteMediaItem().optString(PROPERTY_MEDIA_TYPE, "").startsWith("video") &&
                getNoteMediaItem().has(PROPERTY_MEDIA_URL);
    }

    public boolean containsBadgeMediaType() {
        try {
            JSONArray mediaArray = mNoteData.getJSONArray("media");
            for (int i=0; i < mediaArray.length(); i++) {
                JSONObject mediaObject = mediaArray.getJSONObject(i);
                if (mediaObject.optString(PROPERTY_MEDIA_TYPE, "").equals("badge")) {
                    return true;
                }
            }
        } catch (JSONException e) {
            return false;
        }

        return false;
    }

    public View configureView(final View view) {
        final BasicNoteBlockHolder noteBlockHolder = (BasicNoteBlockHolder)view.getTag();

        // Note image
        if (hasImageMediaItem()) {
            // Request image, and animate it when loaded
            noteBlockHolder.getImageView().setVisibility(View.VISIBLE);
            WordPress.imageLoader.get(getNoteMediaItem().optString("url", ""), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (!mHasAnimatedBadge && response.getBitmap() != null && view.getContext() != null) {
                        mHasAnimatedBadge = true;
                        noteBlockHolder.getImageView().setImageBitmap(response.getBitmap());
                        Animation pop = AnimationUtils.loadAnimation(view.getContext(), R.anim.pop);
                        noteBlockHolder.getImageView().startAnimation(pop);
                        noteBlockHolder.getImageView().setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    noteBlockHolder.hideImageView();
                }
            });
        } else {
            noteBlockHolder.hideImageView();
        }

        // Note video
        if (hasVideoMediaItem()) {
            noteBlockHolder.getVideoView().setVideoURI(Uri.parse(getNoteMediaItem().optString("url", "")));
            noteBlockHolder.getVideoView().setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.hideVideoView();
        }

        // Note text
        if (!TextUtils.isEmpty(getNoteText())) {
            if (mIsBadge) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                noteBlockHolder.getTextView().setLayoutParams(params);
                noteBlockHolder.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
                noteBlockHolder.getTextView().setPadding(0, DisplayUtils.dpToPx(view.getContext(), 8), 0, 0);
            } else {
                noteBlockHolder.getTextView().setGravity(Gravity.NO_GRAVITY);
                noteBlockHolder.getTextView().setPadding(0, 0, 0, 0);
            }
            noteBlockHolder.getTextView().setText(getNoteText());
            noteBlockHolder.getTextView().setVisibility(View.VISIBLE);
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

        private ImageView mImageView;
        private VideoView mVideoView;

        BasicNoteBlockHolder(View view) {
            mRootLayout = (LinearLayout)view;
            mTextView = (WPTextView) view.findViewById(R.id.note_text);
            mTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
        }

        public WPTextView getTextView() {
            return mTextView;
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
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
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
