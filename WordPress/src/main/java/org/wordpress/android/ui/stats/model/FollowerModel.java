package org.wordpress.android.ui.stats.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class FollowerModel implements Serializable {
    private String mBlogId;
    private String mLabel;
    private String mAvatar;
    private String mUrl;
    private String mFollowData;
    private String mDateSubscribed;

    public FollowerModel(String mBlogId, JSONObject followerJSONData) throws JSONException{
        this.mBlogId = mBlogId;
        this.mLabel = followerJSONData.getString("label");
        if (followerJSONData.has("avatar") && !followerJSONData.getString("avatar").equals("null")) {
            setAvatar(followerJSONData.getString("avatar"));
        }
        this.mUrl = followerJSONData.optString("url");
        this.mDateSubscribed = followerJSONData.getString("date_subscribed");
        // Follow data could return a boolean false
        JSONObject followData = followerJSONData.optJSONObject("follow_data");
        setFollowData((followData != null) ? followData.toString() : null);
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getURL() {
        return mUrl;
    }

    public String getFollowData() {
        return mFollowData;
    }

    public void setFollowData(String followData) {
        this.mFollowData = followData;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String icon) {
        this.mAvatar = icon;
    }

    public String getDateSubscribed() {
        return mDateSubscribed;
    }
}