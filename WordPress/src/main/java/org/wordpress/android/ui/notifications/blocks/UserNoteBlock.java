package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.NotificationsConstants;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;

/**
 * A block that displays information about a User (such as a user that liked a post)
 * Will display an action button if available (e.g. follow button)
 */
public class UserNoteBlock extends NoteBlock {
    private final OnGravatarClickedListener mGravatarClickedListener;

    private int mAvatarSz;

    public interface OnGravatarClickedListener {
        // userId is currently unused, but will be handy once a profile view is added to the app
        public void onGravatarClicked(long siteId, long userId);
    }

    public UserNoteBlock(
            Context context,
            JSONObject noteObject,
            OnNoteBlockTextClickListener onNoteBlockTextClickListener,
            OnGravatarClickedListener onGravatarClickedListener) {
        super(noteObject, onNoteBlockTextClickListener);
        if (context != null) {
            setAvatarSize(context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_large));
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

    @Override
    public View configureView(View view) {
        final UserActionNoteBlockHolder noteBlockHolder = (UserActionNoteBlockHolder)view.getTag();
        noteBlockHolder.nameTextView.setText(getNoteText().toString());


        String linkedText = null;
        if (hasUserUrlAndTitle()) {
            linkedText = getUserBlogTitle();
        } else if (hasUserUrl()) {
            linkedText = getUserUrl();
        }

        if (!TextUtils.isEmpty(linkedText)) {
            noteBlockHolder.urlTextView.setText(NotificationsUtils.getClickableTextForIdUrl(
                    getUrlRangeObject(),
                    linkedText,
                    getOnNoteBlockTextClickListener()
            ));
            noteBlockHolder.urlTextView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.urlTextView.setVisibility(View.GONE);
        }

        if (hasUserBlogTagline()) {
            noteBlockHolder.taglineTextView.setText(getUserBlogTagline());
            noteBlockHolder.taglineTextView.setVisibility(View.VISIBLE);
        } else if (hasUserUrlAndTitle()) {
            noteBlockHolder.taglineTextView.setText(getUserUrl());
            noteBlockHolder.taglineTextView.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.taglineTextView.setVisibility(View.GONE);
        }

        if (hasImageMediaItem()) {
            String imageUrl = PhotonUtils.fixAvatar(getNoteMediaItem().optString("url", ""), getAvatarSize());
            noteBlockHolder.avatarImageView.setImageUrl(imageUrl, WordPress.imageLoader);
            if (!TextUtils.isEmpty(getUserUrl())) {
                noteBlockHolder.avatarImageView.setOnTouchListener(mOnGravatarTouchListener);
                noteBlockHolder.rootView.setBackgroundResource(R.drawable.notifications_header_selector);
                noteBlockHolder.rootView.setOnClickListener(mOnClickListener);
            } else {
                noteBlockHolder.avatarImageView.setOnTouchListener(null);
                noteBlockHolder.rootView.setBackgroundColor(NotificationsConstants.COLOR_CALYPSO_WHITE);
                noteBlockHolder.rootView.setOnClickListener(null);
            }
        } else {
            noteBlockHolder.avatarImageView.setImageResource(R.drawable.placeholder);
            noteBlockHolder.avatarImageView.setOnTouchListener(null);
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
        private final View rootView;
        private final TextView nameTextView;
        private final TextView urlTextView;
        private final TextView taglineTextView;
        private final NetworkImageView avatarImageView;

        public UserActionNoteBlockHolder(View view) {
            rootView = view.findViewById(R.id.user_block_root_view);
            nameTextView = (TextView)view.findViewById(R.id.user_name);
            urlTextView = (TextView)view.findViewById(R.id.user_blog_url);
            urlTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            taglineTextView = (TextView)view.findViewById(R.id.user_blog_tagline);
            avatarImageView = (NetworkImageView)view.findViewById(R.id.user_avatar);
        }
    }

    JSONObject getUrlRangeObject() {
        if (getNoteData() == null) return null;

        JSONArray rangesArray = getNoteData().optJSONArray("ranges");
        if (rangesArray != null) {
            for (int i=0; i < rangesArray.length(); i++) {
                try {
                    JSONObject rangeObject = rangesArray.getJSONObject(i);
                    if (rangeObject.has("url")) {
                        return rangeObject;
                    }
                } catch (JSONException e) {
                    AppLog.i(AppLog.T.NOTIFS, "Unexpected object in notifications ids array.");
                }
            }
        }

        return null;
    }

    String getUserUrl() {
        if (getUrlRangeObject() != null) {
            String url = UrlUtils.normalizeUrl(getUrlRangeObject().optString("url", ""));
            return url.replace("http://", "").replace("https://", "");
        }

        return null;
    }

    private String getUserBlogTitle() {
        return JSONUtil.queryJSON(getNoteData(), "meta.titles.home", "");
    }

    private String getUserBlogTagline() {
        return JSONUtil.queryJSON(getNoteData(), "meta.titles.tagline", "");
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
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(animationDuration)
                        .setInterpolator(new DecelerateInterpolator());

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    showBlogPreview();
                }
            }

            return true;
        }
    };

    private void showBlogPreview() {
        long siteId = Long.valueOf(JSONUtil.queryJSON(getNoteData(), "meta.ids.site", 0));
        long userId = Long.valueOf(JSONUtil.queryJSON(getNoteData(), "meta.ids.user", 0));
        if (mGravatarClickedListener != null && siteId > 0 && userId > 0) {
            mGravatarClickedListener.onGravatarClicked(siteId, userId);
        }
    }
}
