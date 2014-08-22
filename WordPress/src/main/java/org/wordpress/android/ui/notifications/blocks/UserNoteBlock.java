package org.wordpress.android.ui.notifications.blocks;

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
import org.wordpress.android.ui.notifications.utils.NotificationUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;

/**
 * A block that displays information about a User (such as a user that liked a post)
 * Will display an action button if available (e.g. follow button)
 */
public class UserNoteBlock extends NoteBlock {
    private OnGravatarClickedListener mGravatarClickedListener;

    private static int GRAVATAR_ANIMATION_DURATION = 150;

    public interface OnGravatarClickedListener {
        public void onGravatarClicked(long userId, long siteId);
    }

    public UserNoteBlock(
            JSONObject noteObject,
            OnNoteBlockTextClickListener onNoteBlockTextClickListener,
            OnGravatarClickedListener onGravatarClickedListener) {
        super(noteObject, onNoteBlockTextClickListener);
        mGravatarClickedListener = onGravatarClickedListener;
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
            noteBlockHolder.urlTextView.setText(NotificationUtils.getClickableTextForIdUrl(
                    getUrlIdObject(),
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
            noteBlockHolder.avatarImageView.setImageUrl(getNoteMediaItem().optString("url", ""), WordPress.imageLoader);
            if (!TextUtils.isEmpty(getUserUrl())) {
                noteBlockHolder.avatarImageView.setOnTouchListener(mOnGravatarTouchListener);
            } else {
                noteBlockHolder.avatarImageView.setOnTouchListener(null);
            }
        } else {
            noteBlockHolder.avatarImageView.setImageResource(R.drawable.placeholder);
            noteBlockHolder.avatarImageView.setOnTouchListener(null);
        }

        return view;
    }

    @Override
    public Object getViewHolder(View view) {
        return new UserActionNoteBlockHolder(view);
    }

    private class UserActionNoteBlockHolder {
        private TextView nameTextView;
        private TextView urlTextView;
        private TextView taglineTextView;
        private NetworkImageView avatarImageView;

        public UserActionNoteBlockHolder(View view) {
            nameTextView = (TextView)view.findViewById(R.id.user_name);
            urlTextView = (TextView)view.findViewById(R.id.user_blog_url);
            urlTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            taglineTextView = (TextView)view.findViewById(R.id.user_blog_tagline);
            avatarImageView = (NetworkImageView)view.findViewById(R.id.user_avatar);
        }
    }

    public JSONObject getUrlIdObject() {
        if (getNoteData() == null) return null;

        JSONArray idsArray = getNoteData().optJSONArray("ids");
        if (idsArray != null) {
            for (int i=0; i < idsArray.length(); i++) {
                try {
                    JSONObject idObject = idsArray.getJSONObject(i);
                    if (idObject.has("url")) {
                        return idObject;
                    }
                } catch (JSONException e) {
                    AppLog.i(AppLog.T.NOTIFS, "Unexpected object in notifications ids array.");
                }
            }
        }

        return null;
    }

    private String getUserUrl() {
        if (getUrlIdObject() != null) {
            String url = UrlUtils.normalizeUrl(getUrlIdObject().optString("url", ""));
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

    private View.OnTouchListener mOnGravatarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .alpha(0.5f)
                        .setDuration(GRAVATAR_ANIMATION_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(GRAVATAR_ANIMATION_DURATION)
                        .setInterpolator(new DecelerateInterpolator());

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    long siteId = Long.valueOf(JSONUtil.queryJSON(getNoteData(), "meta.ids.site", 0));
                    long userId = Long.valueOf(JSONUtil.queryJSON(getNoteData(), "meta.ids.user", 0));
                    if (mGravatarClickedListener != null && siteId > 0 && userId > 0) {
                        mGravatarClickedListener.onGravatarClicked(siteId, userId);
                    }
                }
            }

            return true;
        }
    };
}
