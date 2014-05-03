package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderBlogInfo {
    public long blogId;
    public boolean isPrivate;
    public boolean isJetpack;
    public boolean isFollowing;
    public int numSubscribers;

    private String name;
    private String description;
    private String url;

    /*{
    "ID": 3584907,
    "name": "WordPress.com News",
    "description": "The latest news on WordPress.com and the WordPress community.",
    "URL": "http:\/\/en.blog.wordpress.com",
    "jetpack": false,
    "subscribers_count": 9222924,
    "is_private": false,
    "is_following": false,
    "meta": {
        "links": {
            "self": "https:\/\/public-api.wordpress.com\/rest\/v1\/sites\/3584907",
            "help": "https:\/\/public-api.wordpress.com\/rest\/v1\/sites\/3584907\/help",
            "posts": "https:\/\/public-api.wordpress.com\/rest\/v1\/sites\/3584907\/posts\/",
            "comments": "https:\/\/public-api.wordpress.com\/rest\/v1\/sites\/3584907\/comments\/"
        }
    }
    }*/

    public static ReaderBlogInfo fromJson(JSONObject json) {
        ReaderBlogInfo blog = new ReaderBlogInfo();
        if (json == null) {
            return blog;
        }

        blog.blogId = json.optLong("ID");

        blog.setName(JSONUtil.getStringDecoded(json, "name"));
        blog.setDescription(JSONUtil.getStringDecoded(json, "description"));
        blog.setUrl(JSONUtil.getString(json, "URL"));

        blog.isJetpack = JSONUtil.getBool(json, "jetpack");
        blog.isPrivate = JSONUtil.getBool(json, "is_private");
        blog.isFollowing = JSONUtil.getBool(json, "is_following");
        blog.numSubscribers = json.optInt("subscribers_count");

        return blog;
    }

    /*
     * info is considered incomplete if it has no blog id, or it's missing both the name and
     * description - used by ReaderBlogAction.updateIncompleteBlogInfo() to fill in incomplete
     * blogInfo, and by ReaderBlogInfoList.removeIncomplete()
     */
    public boolean isIncomplete() {
        return (!hasBlogId() || (!hasName() && !hasDescription()));
    }

    public String getName() {
        return StringUtils.notNullStr(name);
    }
    public void setName(String blogName) {
        this.name = StringUtils.notNullStr(blogName).trim();
    }

    public String getDescription() {
        return StringUtils.notNullStr(description);
    }
    public void setDescription(String description) {
        this.description = StringUtils.notNullStr(description).trim();
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public boolean hasBlogId() {
        return (blogId != 0);
    }
    public boolean hasUrl() {
        return !TextUtils.isEmpty(url);
    }
    public boolean hasName() {
        return !TextUtils.isEmpty(name);
    }
    public boolean hasDescription() {
        return !TextUtils.isEmpty(description);
    }

    public String getMshotsUrl(int width) {
        return "http://s.wordpress.com/mshots/v1/"
             + UrlUtils.urlEncode(getUrl())
             + "?w=" + Integer.toString(width);
    }

    protected boolean isSameAs(ReaderBlogInfo blogInfo) {
        if (blogInfo == null) {
            return false;
        }
        if (this.blogId != blogInfo.blogId) {
            return false;
        }
        if (this.isFollowing != blogInfo.isFollowing) {
            return false;
        }
        if (this.numSubscribers != blogInfo.numSubscribers) {
            return false;
        }
        if (!this.getName().equals(blogInfo.getName())) {
            return false;
        }
        if (!this.getDescription().equals(blogInfo.getDescription())) {
            return false;
        }
        if (!this.getUrl().equals(blogInfo.getUrl())) {
            return false;
        }
        return true;
    }
}
