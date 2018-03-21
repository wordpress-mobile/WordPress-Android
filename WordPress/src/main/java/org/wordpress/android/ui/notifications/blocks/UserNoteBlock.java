package org.wordpress.android.ui.notifications.blocks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * A block that displays information about a User (such as a user that liked a post)
 */
public class UserNoteBlock extends NoteBlock {
    private final OnGravatarClickedListener mGravatarClickedListener;

    private int mAvatarSz;

    public interface OnGravatarClickedListener {
        // userId is currently unused, but will be handy once a profile view is added to the app
        void onGravatarClicked(long siteId, long userId, String siteUrl);
    }

    public UserNoteBlock(
            Context context,
            JSONObject noteObject,
            OnNoteBlockTextClickListener onNoteBlockTextClickListener,
            OnGravatarClickedListener onGravatarClickedListener) {
        super(noteObject, onNoteBlockTextClickListener);
        if (context != null) {
            setAvatarSize(context.getResources().getDimensionPixelSize(R.dimen.notifications_avatar_sz));
        }
        mGravatarClickedListener = onGravatarClickedListener;
    }

    void setAvatarSize(int size) {
        mAvatarSz = size;
    }

    int getAvatarSize() {
        return mAvatarSz;
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.USER;
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_user;
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    @Override
    public View configureView(View view) {
        final UserActionNoteBlockHolder noteBlockHolder = (UserActionNoteBlockHolder) view.getTag();
        noteBlockHolder.mNameTextView.setText(getNoteText().toString());


        String linkedText = null;
        if (hasUserUrlAndTitle()) {
            linkedText = getUserBlogTitle();
        } else if (hasUserUrl()) {
            linkedText = getUserUrl();
        }

        if (!TextUtils.isEmpty(linkedText)) {
            noteBlockHolder.mUrlTextView.setText(linkedText);
            noteBlockHolder.mUrlTextView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.mUrlTextView.setVisibility(View.GONE);
        }

        if (hasUserBlogTagline()) {
            noteBlockHolder.mTaglineTextView.setText(getUserBlogTagline());
            noteBlockHolder.mTaglineTextView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.mTaglineTextView.setVisibility(View.GONE);
        }

        if (hasImageMediaItem()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(getNoteMediaItem().optString("url", ""), getAvatarSize());
            noteBlockHolder.mAvatarImageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.AVATAR);
            if (!TextUtils.isEmpty(getUserUrl())) {
                //noinspection AndroidLintClickableViewAccessibility
                noteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener);
                noteBlockHolder.mRootView.setEnabled(true);
                noteBlockHolder.mRootView.setOnClickListener(mOnClickListener);
            } else {
                //noinspection AndroidLintClickableViewAccessibility
                noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
                noteBlockHolder.mRootView.setEnabled(false);
                noteBlockHolder.mRootView.setOnClickListener(null);
            }
        } else {
            noteBlockHolder.mRootView.setEnabled(false);
            noteBlockHolder.mAvatarImageView.showDefaultGravatarImageAndNullifyUrl();
            noteBlockHolder.mRootView.setOnClickListener(null);
            //noinspection AndroidLintClickableViewAccessibility
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
        }

        return view;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showBlogPreview();
        }
    };

    @Override
    public Object getViewHolder(View view) {
        return new UserActionNoteBlockHolder(view);
    }

    private class UserActionNoteBlockHolder {
        private final View mRootView;
        private final TextView mNameTextView;
        private final TextView mUrlTextView;
        private final TextView mTaglineTextView;
        private final WPNetworkImageView mAvatarImageView;

        UserActionNoteBlockHolder(View view) {
            mRootView = view.findViewById(R.id.user_block_root_view);
            mNameTextView = (TextView) view.findViewById(R.id.user_name);
            mUrlTextView = (TextView) view.findViewById(R.id.user_blog_url);
            mTaglineTextView = (TextView) view.findViewById(R.id.user_blog_tagline);
            mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.user_avatar);
        }
    }

    String getUserUrl() {
        return JSONUtils.queryJSON(getNoteData(), "meta.links.home", "");
    }

    private String getUserBlogTitle() {
        return JSONUtils.queryJSON(getNoteData(), "meta.titles.home", "");
    }

    private String getUserBlogTagline() {
        return JSONUtils.queryJSON(getNoteData(), "meta.titles.tagline", "");
    }

    private boolean hasUserUrl() {
        return !TextUtils.isEmpty(getUserUrl());
    }

    private boolean hasUserUrlAndTitle() {
        return hasUserUrl() && !TextUtils.isEmpty(getUserBlogTitle());
    }

    private boolean hasUserBlogTagline() {
        return !TextUtils.isEmpty(getUserBlogTagline());
    }

    final View.OnTouchListener mOnGravatarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int animationDuration = 150;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                 .scaleX(0.9f)
                 .scaleY(0.9f)
                 .alpha(0.5f)
                 .setDuration(animationDuration)
                 .setInterpolator(new DecelerateInterpolator());
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                       || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                 .scaleX(1.0f)
                 .scaleY(1.0f)
                 .alpha(1.0f)
                 .setDuration(animationDuration)
                 .setInterpolator(new DecelerateInterpolator());

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    v.performClick();
                }
            }

            return true;
        }
    };

    protected void showBlogPreview() {
        long siteId = Long.valueOf(JSONUtils.queryJSON(getNoteData(), "meta.ids.site", 0));
        long userId = Long.valueOf(JSONUtils.queryJSON(getNoteData(), "meta.ids.user", 0));
        String siteUrl = getUserUrl();
        if (mGravatarClickedListener != null) {
            mGravatarClickedListener.onGravatarClicked(siteId, userId, siteUrl);
        }
    }
}
