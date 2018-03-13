package org.wordpress.android.ui.stats.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.Serializable;

public class FollowerModel implements Serializable {
    private long mBlogId;
    private String mLabel;
    private String mAvatar;
    private String mUrl;
    private FollowDataModel mFollowData;
    private String mDateSubscribed;

    public FollowerModel(long mBlogId, JSONObject followerJSONData) throws JSONException {
        this.mBlogId = mBlogId;
        this.mLabel = followerJSONData.getString("label");

        setAvatar(JSONUtils.getString(followerJSONData, "avatar"));
        setURL(JSONUtils.getString(followerJSONData, "url"));

        this.mDateSubscribed = followerJSONData.getString("date_subscribed");

        JSONObject followData = followerJSONData.optJSONObject("follow_data");
        if (followData != null) {
            this.mFollowData = new FollowDataModel(followData);
        }
    }

    public long getBlogId() {
        return mBlogId;
    }

    public void setBlogId(long blogId) {
        this.mBlogId = blogId;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getURL() {
        return mUrl;
    }

    private boolean setURL(String url) {
        if (!TextUtils.isEmpty(url) && UrlUtils.isValidUrlAndHostNotNull(url)) {
            this.mUrl = url;
            return true;
        }
        return false;
    }

    public FollowDataModel getFollowData() {
        return mFollowData;
    }

    public String getAvatar() {
        return mAvatar;
    }

    private void setAvatar(String icon) {
        this.mAvatar = icon;
    }

    public String getDateSubscribed() {
        return mDateSubscribed;
    }
}
