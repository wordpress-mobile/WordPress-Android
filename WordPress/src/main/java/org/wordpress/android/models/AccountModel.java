package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Date;

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
    private Date mDateCreated;
    private String mNewEmail;
    private boolean mPendingEmailChange;
    private String mWebAddress;

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
        mDateCreated = new Date();
        mNewEmail = "";
        mPendingEmailChange = false;
        mWebAddress = "";
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

        Date date = DateTimeUtils.iso8601ToJavaDate(json.optString("date"));
        if (date != null) {
            mDateCreated = date;
        } else {
            AppLog.e(AppLog.T.API, "Date could not be found from Account JSON response");
        }
    }

    public void updateAccountSettingsFromRestResponse(JSONObject json) {
        if (json.has(RestParam.FIRST_NAME.getDescription())) mFirstName = json.optString(RestParam.FIRST_NAME.getDescription());
        if (json.has(RestParam.LAST_NAME.getDescription())) mLastName = json.optString(RestParam.LAST_NAME.getDescription());
        if (json.has(RestParam.DISPLAY_NAME.getDescription())) mDisplayName = json.optString(RestParam.DISPLAY_NAME.getDescription());
        if (json.has(RestParam.ABOUT_ME.getDescription())) mAboutMe = json.optString(RestParam.ABOUT_ME.getDescription());
        if (json.has(RestParam.EMAIL.getDescription())) mEmail = json.optString(RestParam.EMAIL.getDescription());
        if (json.has(RestParam.NEW_EMAIL.getDescription())) mNewEmail = json.optString(RestParam.NEW_EMAIL.getDescription());
        if (json.has(RestParam.EMAIL_CHANGE_PENDING.getDescription())) mPendingEmailChange = json.optBoolean(RestParam.EMAIL_CHANGE_PENDING.getDescription());
        if (json.has(RestParam.PRIMARY_BLOG.getDescription())) mPrimaryBlogId = json.optLong(RestParam.PRIMARY_BLOG.getDescription());
        if (json.has(RestParam.WEB_ADDRESS.getDescription())) mWebAddress = json.optString(RestParam.WEB_ADDRESS.getDescription());
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

    public Date getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(Date date) {
        mDateCreated = date;
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

    public String getWebAddress() {
        return mWebAddress;
    }

    public void setWebAddress(String webAddress) {
        mWebAddress = webAddress;
    }

    public enum RestParam {
        FIRST_NAME("first_name"),
        LAST_NAME("last_name"),
        DISPLAY_NAME("display_name"),
        ABOUT_ME("description"),
        EMAIL("user_email"),
        NEW_EMAIL("new_user_email"),
        EMAIL_CHANGE_PENDING("user_email_change_pending"),
        PRIMARY_BLOG("primary_site_ID"),
        WEB_ADDRESS("user_URL");

        private String description;

        RestParam(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
