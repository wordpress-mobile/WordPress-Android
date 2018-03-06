package org.wordpress.android.ui.stats.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class FollowDataModel implements Serializable {
       /*
     "following-text": "Following",
      "is_following": false,
      "following-hover-text": "Unfollow",
      "blog_id": 6098762,
      "blog_url": "http://ilpostodellefragole.wordpress.com",
      "blog_title": "Il posto delle fragole",
      "site_id": 6098762,
      "stat-source": "stats_comments",
      "follow-text": "Follow",
      "blog_domain": "ilpostodellefragole.wordpress.com"
     */

    private String mType;
    private String mFollowText;
    private String mFollowingText;
    private String mFollowingHoverText;
    private boolean mIsFollowing;
    private int mBlogID;
    private int mSiteID;
    private String mStatsSource;
    private String mBlogDomain;

    public transient boolean isRestCallInProgress = false;

    public FollowDataModel(JSONObject followDataJSON) throws JSONException {
        mType = followDataJSON.getString("type");
        JSONObject paramsJSON = followDataJSON.getJSONObject("params");
        mFollowText = paramsJSON.getString("follow-text");
        mFollowingText = paramsJSON.getString("following-text");
        mFollowingHoverText = paramsJSON.getString("following-hover-text");
        mIsFollowing = paramsJSON.getBoolean("is_following");
        mBlogID = paramsJSON.getInt("blog_id");
        mSiteID = paramsJSON.getInt("site_id");
        mStatsSource = paramsJSON.getString("stat-source");
        mBlogDomain = paramsJSON.getString("blog_domain");
    }

    public boolean isFollowing() {
        return mIsFollowing;
    }

    public void setIsFollowing(boolean following) {
        mIsFollowing = following;
    }

    public int getBlogID() {
        return mBlogID;
    }

    public int getSiteID() {
        return mSiteID;
    }

    public String getFollowText() {
        return mFollowText;
    }

    public String getFollowingHoverText() {
        return mFollowingHoverText;
    }

    public String getFollowingText() {
        return mFollowingText;
    }

    public String getType() {
        return mType;
    }

    public String getStatsSource() {
        return mStatsSource;
    }

    public String getBlogDomain() {
        return mBlogDomain;
    }
}
