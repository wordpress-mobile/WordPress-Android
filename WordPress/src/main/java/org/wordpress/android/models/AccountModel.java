package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.StringUtils;

public class AccountModel {
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
    private String mEmail;
    private String mFirstName;
    private String mLastName;
    private String mAboutMe;
    private String mNewEmail;
    private boolean mPendingEmailChange;

    public AccountModel() {
        init();
    }

    public void init() {
        mUserName = "";
        mUserId = 0;
        mDisplayName = "";
        mProfileUrl = "";
        mAvatarUrl = "";
        mPrimaryBlogId = 0;
        mSiteCount = 0;
        mVisibleSiteCount = 0;
        mAccessToken = "";
        mEmail = "";
        mFirstName = "";
        mLastName = "";
        mAboutMe = "";
        mNewEmail = "";
        mPendingEmailChange = false;
    }

    public void updateFromRestResponse(JSONObject json) {
        mUserId = json.optLong("ID");
        mUserName = json.optString("username");
        mDisplayName = json.optString("display_name");
        mProfileUrl = json.optString("profile_URL");
        mAvatarUrl = json.optString("avatar_URL");
        mPrimaryBlogId = json.optLong("primary_blog");
        mSiteCount = json.optInt("site_count");
        mVisibleSiteCount = json.optInt("visible_site_count");
        mEmail = json.optString("email");
    }

    public void updateAccountSettingsFromRestResponse(JSONObject json) {
        mFirstName = json.optString("first_name");
        mLastName = json.optString("last_name");
        mDisplayName = json.optString("display_name");
        mAboutMe = json.optString("description");
        mNewEmail = json.optString("new_user_email");
        mPendingEmailChange = json.optBoolean("user_email_change_pending");
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        mUserId = userId;
    }

    public void setPrimaryBlogId(long primaryBlogId) {
        mPrimaryBlogId = primaryBlogId;
    }

    public long getPrimaryBlogId() {
        return mPrimaryBlogId;
    }

    public String getUserName() {
        return StringUtils.notNullStr(mUserName);
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    boolean hasAccessToken() {
        return !TextUtils.isEmpty(getAccessToken());
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(mDisplayName);
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getProfileUrl() {
        return StringUtils.notNullStr(mProfileUrl);
    }

    public void setProfileUrl(String profileUrl) {
        mProfileUrl = profileUrl;
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(mAvatarUrl);
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

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getEmail() {
        return StringUtils.notNullStr(mEmail);
    }

    public String getFirstName() {
        return StringUtils.notNullStr(mFirstName);
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getLastName() {
        return StringUtils.notNullStr(mLastName);
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getAboutMe() {
        return StringUtils.notNullStr(mAboutMe);
    }

    public void setAboutMe(String aboutMe) {
        mAboutMe = aboutMe;
    }

    public String getNewEmail() {
        return StringUtils.notNullStr(mNewEmail);
    }

    public void setNewEmail(String newEmail) {
        mNewEmail = newEmail;
    }

    public boolean getPendingEmailChange() {
        return mPendingEmailChange;
    }

    public void setPendingEmailChange(boolean pendingEmailChange) {
        mPendingEmailChange = pendingEmailChange;
    }
}
