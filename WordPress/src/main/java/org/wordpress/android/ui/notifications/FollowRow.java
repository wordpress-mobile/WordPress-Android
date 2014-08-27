/**
 * A row with and avatar, name and follow button
 *
 * The follow button switches between "Follow" and "Unfollow" depending on the follow status
 * and provides and interface to know when the user has tried to follow or unfollow by tapping
 * the button.
 *
 * Potentially can integrate with Gravatar using the avatar url to find profile JSON.
 */
package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.HtmlUtils;

public class FollowRow extends LinearLayout {
    public static interface OnFollowListener {
        public void onUnfollow(FollowRow row, String blogId);
        public void onFollow(FollowRow row, String blogId);
    }

    private static final String PARAMS_FIELD       = "params";
    private static final String TYPE_FIELD         = "type";
    private static final String ACTION_TYPE        = "follow";
    private static final String BLOG_ID_PARAM      = "blog_id";
    private static final String IS_FOLLOWING_PARAM = "is_following";
    private static final String BLOG_URL_PARAM     = "blog_url";
    private static final String BLOG_DOMAIN_PARAM  = "blog_domain";

    private OnFollowListener mFollowListener;
    private JSONObject mParams;
    private String mBlogURL;

    public FollowRow(Context context) {
        super(context);
    }

    public FollowRow(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    public FollowRow(Context context, AttributeSet attributes, int defStyle) {
        super(context, attributes, defStyle);
    }

    void setAction(JSONObject actionJSON) {
        final TextView followButton = getFollowButton();

        getImageView().setDefaultImageResId(R.drawable.placeholder);
        try {
            if (actionJSON.has(TYPE_FIELD) && actionJSON.getString(TYPE_FIELD).equals(ACTION_TYPE)) {
                // get the params for following
                mParams = actionJSON.getJSONObject(PARAMS_FIELD);
                // show the button
                followButton.setVisibility(VISIBLE);
                followButton.setOnClickListener(new ClickListener());
                followButton.setOnLongClickListener(new LongClickListener());
                setClickable(true);
            } else {
                mParams = null;
                followButton.setVisibility(GONE);
                followButton.setOnClickListener(null);
                setClickable(false);
            }

            if (hasParams()) {
                setSiteUrl(mParams.optString(BLOG_URL_PARAM, null));
            } else {
                setSiteUrl(null);
            }

            updateFollowButton(isFollowing());

        } catch (JSONException e) {
            AppLog.e(T.NOTIFS, String.format("Could not set action from %s", actionJSON), e);
            followButton.setVisibility(GONE);
            setSiteUrl(null);
            setClickable(false);
            mParams = null;
        }
    }

    private boolean hasParams() {
        return mParams != null;
    }

    NetworkImageView getImageView() {
        return (NetworkImageView) findViewById(R.id.avatar);
    }

    TextView getFollowButton() {
        return (TextView) findViewById(R.id.text_follow);
    }

    TextView getNameTextView() {
        return (TextView) findViewById(R.id.name);
    }

    TextView getSiteTextView() {
        return (TextView) findViewById(R.id.url);
    }

    void setNameText(String text) {
        TextView nameText = getNameTextView();
        if (TextUtils.isEmpty(text)) {
            nameText.setVisibility(View.GONE);
        } else {
            // text may contain html entities, so it must be unescaped for display
            nameText.setText(HtmlUtils.fastUnescapeHtml(text));
            nameText.setVisibility(View.VISIBLE);
        }
    }

    boolean isSiteId(String siteId) {
        String thisSiteId = getSiteId();
        return (thisSiteId != null && thisSiteId.equals(siteId));
    }

    void setFollowing(boolean following) {
        if (hasParams()) {
            try {
                mParams.putOpt(IS_FOLLOWING_PARAM, following);
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, String.format("Could not set following %b", following), e);
            }
        }
        updateFollowButton(following);
    }

    boolean isFollowing() {
        return hasParams() && mParams.optBoolean(IS_FOLLOWING_PARAM, false);
    }

    String getSiteId() {
        if (hasParams()) {
            return mParams.optString(BLOG_ID_PARAM, null);
        } else {
            return null;
        }
    }

    void setSiteUrl(String url) {
        mBlogURL = url;
        final TextView siteTextView = getSiteTextView();

        if (!TextUtils.isEmpty(url)) {
            siteTextView.setText(getSiteDomain());
            siteTextView.setVisibility(View.VISIBLE);
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBlogURL != null) {
                        NotificationsWebViewActivity.openUrl(getContext(), mBlogURL);
                    }
                }
            });
        } else {
            this.setOnClickListener(null);
            siteTextView.setVisibility(View.GONE);
        }
    }

    private String getSiteDomain() {
        if (hasParams()) {
            return mParams.optString(BLOG_DOMAIN_PARAM, null);
        } else {
            return null;
        }
    }

    private OnFollowListener getFollowListener() {
        return mFollowListener;
    }

    void setFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    private boolean hasFollowListener() {
        return mFollowListener != null;
    }

    private void updateFollowButton(boolean isFollowing) {
        final TextView followButton = getFollowButton();
        int drawableId = (isFollowing ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        followButton.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
        followButton.setSelected(isFollowing);
        followButton.setText(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
    }

    private class ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            if (!hasFollowListener()) {
                return;
            }

            // first make sure we have a connection
            if (!NetworkUtils.checkConnection(getContext()))
                return;

            // show new follow state and animate button right away (before network call)
            updateFollowButton(!isFollowing());
            ReaderAnim.animateFollowButton(getFollowButton());

            if (isFollowing()) {
                getFollowListener().onUnfollow(FollowRow.this, getSiteId());
            } else {
                getFollowListener().onFollow(FollowRow.this, getSiteId());
            }
        }
    }

    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(getContext(), getResources().getString(R.string.tooltip_follow), Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}