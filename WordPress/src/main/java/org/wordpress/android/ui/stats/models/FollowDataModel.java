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

    private String type;
    private String followText;
    private String followingText;
    private String followingHoverText;
    private boolean isFollowing;
    private int blogID;
    private int siteID;
    private String statsSource;
    private String blogDomain;

    public transient boolean isRestCallInProgress = false;

    public FollowDataModel(JSONObject followDataJSON) throws JSONException {
        this.type = followDataJSON.getString("type");
        JSONObject paramsJSON = followDataJSON.getJSONObject("params");
        this.followText = paramsJSON.getString("follow-text");
        this.followingText = paramsJSON.getString("following-text");
        this.followingHoverText = paramsJSON.getString("following-hover-text");
        this.isFollowing = paramsJSON.getBoolean("is_following");
        this.blogID = paramsJSON.getInt("blog_id");
        this.siteID = paramsJSON.getInt("site_id");
        this.statsSource = paramsJSON.getString("stat-source");
        this.blogDomain = paramsJSON.getString("blog_domain");
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(boolean following) {
        isFollowing = following;
    }

    public int getBlogID() {
        return blogID;
    }

    public int getSiteID() {
        return siteID;
    }

    public String getFollowText() {
        return followText;
    }

    public String getFollowingHoverText() {
        return followingHoverText;
    }

    public String getFollowingText() {
        return followingText;
    }

    public String getType() {
        return type;
    }

    public String getStatsSource() {
        return statsSource;
    }

    public String getBlogDomain() {
        return blogDomain;
    }
}
