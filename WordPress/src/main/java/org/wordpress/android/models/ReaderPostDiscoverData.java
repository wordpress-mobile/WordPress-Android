package org.wordpress.android.models;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.StringUtils;

/**
 * additional data for "discovery" posts in the reader
 */
public class ReaderPostDiscoverData {
    private String authorName;
    private String authorUrl;
    private String blogName;
    private String blogUrl;
    private String avatarUrl;
    private String permaLink;

    private long blogId;
    private long postId;
    private int numLikes;
    private int numComments;

    /*
     * passed JSONObject is the "discover_metadata" section of a reader post
     */
    public ReaderPostDiscoverData(@NonNull String jsonString) throws JSONException {
        this(new JSONObject(jsonString));
    }
    public ReaderPostDiscoverData(@NonNull JSONObject json) {
        setPermaLink(json.optString("permalink"));

        JSONObject jsonAttribution = json.optJSONObject("attribution");
        if (jsonAttribution != null) {
            setAuthorName(jsonAttribution.optString("author_name"));
            setAuthorUrl(jsonAttribution.optString("author_url"));
            setBlogName(jsonAttribution.optString("blog_name"));
            setBlogUrl(jsonAttribution.optString("blog_url"));
            setAvatarUrl(jsonAttribution.optString("avatar_url"));
        }

        JSONObject jsonWpcomData = json.optJSONObject("featured_post_wpcom_data");
        if (jsonWpcomData != null) {
            blogId = jsonWpcomData.optLong("blog_id");
            postId = jsonWpcomData.optLong("post_id");
            numLikes = jsonWpcomData.optInt("like_count");
            numComments = jsonWpcomData.optInt("comment_count");
        }
    }

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }
    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(blogUrl);
    }
    public void setBlogUrl(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    public String getAvatarUrl() {
        return StringUtils.notNullStr(avatarUrl);
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPermaLink() {
        return StringUtils.notNullStr(permaLink);
    }
    public void setPermaLink(String permaLink) {
        this.permaLink = permaLink;
    }
}
