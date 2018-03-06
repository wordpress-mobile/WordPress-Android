package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderUser {
    public long userId;
    public long blogId;
    private String mUserName;
    private String mDisplayName;
    private String mUrl;
    private String mProfileUrl;
    private String mAvatarUrl;

    public static ReaderUser fromJson(JSONObject json) {
        ReaderUser user = new ReaderUser();
        if (json == null) {
            return user;
        }

        user.userId = json.optLong("ID");
        user.blogId = json.optLong("site_ID");

        user.mUserName = JSONUtils.getString(json, "username");
        user.mUrl = JSONUtils.getString(json, "URL"); // <-- this isn't necessarily a wp blog
        user.mProfileUrl = JSONUtils.getString(json, "profile_URL");
        user.mAvatarUrl = JSONUtils.getString(json, "avatar_URL");

        // "me" api call (current user) has "display_name", others have "name"
        if (json.has("display_name")) {
            user.mDisplayName = JSONUtils.getStringDecoded(json, "display_name");
        } else {
            user.mDisplayName = JSONUtils.getStringDecoded(json, "name");
        }

        return user;
    }

    public String getUserName() {
        return StringUtils.notNullStr(mUserName);
    }

    public void setUserName(String userName) {
        this.mUserName = StringUtils.notNullStr(userName);
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(mDisplayName);
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = StringUtils.notNullStr(displayName);
    }

    public String getUrl() {
        return StringUtils.notNullStr(mUrl);
    }

    public void setUrl(String url) {
        this.mUrl = StringUtils.notNullStr(url);
    }

    public String getProfileUrl() {
        return StringUtils.notNullStr(mProfileUrl);
    }

    public void setProfileUrl(String profileUrl) {
        this.mProfileUrl = StringUtils.notNullStr(profileUrl);
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(mAvatarUrl);
    }

    public void setAvatarUrl(String avatarUrl) {
        this.mAvatarUrl = StringUtils.notNullStr(avatarUrl);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(mUrl);
    }

    public boolean hasAvatarUrl() {
        return !TextUtils.isEmpty(mAvatarUrl);
    }

    public boolean hasBlogId() {
        return (blogId != 0);
    }

    /*
     * not stored - used by ReaderUserAdapter for performance
     */
    private transient String mUrlDomain;

    public String getUrlDomain() {
        if (mUrlDomain == null) {
            if (hasUrl()) {
                mUrlDomain = UrlUtils.getHost(getUrl());
            } else {
                mUrlDomain = "";
            }
        }
        return mUrlDomain;
    }

    public boolean isSameUser(ReaderUser user) {
        if (user == null) {
            return false;
        }
        if (this.userId != user.userId) {
            return false;
        }
        if (!this.getAvatarUrl().equals(user.getAvatarUrl())) {
            return false;
        }
        if (!this.getDisplayName().equals(user.getDisplayName())) {
            return false;
        }
        if (!this.getUserName().equals(user.getUserName())) {
            return false;
        }
        if (!this.getUrl().equals(user.getUrl())) {
            return false;
        }
        if (!this.getProfileUrl().equals(user.getProfileUrl())) {
            return false;
        }
        return true;
    }
}
