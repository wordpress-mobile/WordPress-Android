package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.WordPress;

/**
 * Class for managing logged in user informations.
 */
public class Account {
    // General state
    private boolean mIsLoggedIn;
    private boolean mIsWordPressComUser;

    // WordPress.com only - data fetched from the REST API endpoint
    private String mUserName;
    private long mUserId;
    private String mDisplayName;
    private String mProfileUrl;
    private String mAvatarUrl;
    private long mPrimaryBlogId;
    private int mSiteCount;
    private int mVisibleSiteCount;
    private String mAccessToken;

    public Account() {
        init();
    }

    public void init() {
        mIsLoggedIn = false;
        mIsWordPressComUser = false;
        mUserName = "";
        mUserId = 0;
        mDisplayName = "";
        mProfileUrl = "";
        mAvatarUrl = "";
        mPrimaryBlogId = 0;
        mSiteCount = 0;
        mVisibleSiteCount = 0;
        mAccessToken = "";
    }

    public void updateFromRestResponse(JSONObject json) {
        mIsWordPressComUser = true;
        mIsLoggedIn = true;
        mUserId = json.optLong("ID");
        mUserName = json.optString("username");
        mDisplayName = json.optString("display_name");
        mProfileUrl = json.optString("profile_URL");
        mAvatarUrl = json.optString("avatar_URL");
        mPrimaryBlogId = json.optLong("primary_blog");
        mSiteCount = json.optInt("site_count");
        mVisibleSiteCount = json.optInt("visible_site_count");
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        mUserId = userId;
    }

    public boolean isSignedIn() {
        return mIsLoggedIn;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        mIsLoggedIn = isLoggedIn;
    }

    public void setPrimaryBlogId(long primaryBlogId) {
        mPrimaryBlogId = primaryBlogId;
    }

    public long getPrimaryBlogId() {
        return mPrimaryBlogId;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public boolean isJetPackUser() {
        return WordPress.wpDB.hasAnyJetpackBlogs();
    }

    public boolean isWordPressComUser() {
        return mIsWordPressComUser;
    }

    public void setIsWordPressComUser(boolean isWordPressComUser) {
        mIsWordPressComUser = isWordPressComUser;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getProfileUrl() {
        return mProfileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        mProfileUrl = profileUrl;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    public int getSiteCount() {
        return mSiteCount;
    }

    public void setSiteCount(int siteCount) {
        mSiteCount = siteCount;
    }

    public int getVisibleSiteCount() {
        return mVisibleSiteCount;
    }

    public void setVisibleSiteCount(int visibleSiteCount) {
        mVisibleSiteCount = visibleSiteCount;
    }
}
