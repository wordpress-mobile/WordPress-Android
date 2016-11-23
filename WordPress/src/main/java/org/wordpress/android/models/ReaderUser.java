package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderUser {
    public long userId;
    public long blogId;
    private String userName;
    private String displayName;
    private String url;
    private String profileUrl;
    private String avatarUrl;

    public static ReaderUser fromJson(JSONObject json) {
        ReaderUser user = new ReaderUser();
        if (json==null)
            return user;

        user.userId = json.optLong("ID");
        user.blogId = json.optLong("site_ID");

        user.userName = JSONUtils.getString(json, "username");
        user.url = JSONUtils.getString(json, "URL"); // <-- this isn't necessarily a wp blog
        user.profileUrl = JSONUtils.getString(json, "profile_URL");
        user.avatarUrl = JSONUtils.getString(json, "avatar_URL");

        // "me" api call (current user) has "display_name", others have "name"
        if (json.has("display_name")) {
            user.displayName = JSONUtils.getStringDecoded(json, "display_name");
        } else {
            user.displayName = JSONUtils.getStringDecoded(json, "name");
        }

        return user;
    }

    public String getUserName() {
        return StringUtils.notNullStr(userName);
    }
    public void setUserName(String userName) {
        this.userName = StringUtils.notNullStr(userName);
    }

    public String getDisplayName() {
        return StringUtils.notNullStr(displayName);
    }
    public void setDisplayName(String displayName) {
        this.displayName = StringUtils.notNullStr(displayName);
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getProfileUrl() {
        return StringUtils.notNullStr(profileUrl);
    }
    public void setProfileUrl(String profileUrl) {
        this.profileUrl = StringUtils.notNullStr(profileUrl);
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = StringUtils.notNullStr(avatarUrl);
    }

    public boolean hasUrl() {
        return !TextUtils.isEmpty(url);
    }

    public boolean hasAvatarUrl() {
        return !TextUtils.isEmpty(avatarUrl);
    }

    public boolean hasBlogId() {
        return (blogId != 0);
    }

    /*
     * not stored - used by ReaderUserAdapter for performance
     */
    private transient String urlDomain;
    public String getUrlDomain() {
        if (urlDomain == null) {
            if (hasUrl()) {
                urlDomain = UrlUtils.getHost(getUrl());
            } else {
                urlDomain = "";
            }
        }
        return urlDomain;
    }

    public boolean isSameUser(ReaderUser user) {
        if (user == null)
            return false;
        if (this.userId != user.userId)
            return false;
        if (!this.getAvatarUrl().equals(user.getAvatarUrl()))
            return false;
        if (!this.getDisplayName().equals(user.getDisplayName()))
            return false;
        if (!this.getUserName().equals(user.getUserName()))
            return false;
        if (!this.getUrl().equals(user.getUrl()))
            return false;
        if (!this.getProfileUrl().equals(user.getProfileUrl()))
            return false;
        return true;
    }
}
