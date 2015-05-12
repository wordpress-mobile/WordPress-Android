package org.wordpress.android.ui.stats.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.Serializable;

public class FollowerModel implements Serializable {
    private String mBlogId;
    private String mLabel;
    private String mAvatar;
    private String mUrl;
    private FollowDataModel mFollowData;
    private String mDateSubscribed;

    public FollowerModel(String mBlogId, JSONObject followerJSONData) throws JSONException{
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

    private boolean setURL(String URL) {
        if (!TextUtils.isEmpty(URL) && UrlUtils.isValidUrlAndHostNotNull(URL)) {
            this.mUrl = URL;
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
