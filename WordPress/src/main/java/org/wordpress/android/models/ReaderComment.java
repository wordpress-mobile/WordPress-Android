package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

public class ReaderComment {
    public long commentId;
    public long blogId;
    public long postId;
    public long parentId;

    private String authorName;
    private String authorAvatar;
    private String authorUrl;
    private String status;
    private String text;

    private String published;
    public long timestamp;

    public long authorId;
    public long authorBlogId;

    public int numLikes;
    public boolean isLikedByCurrentUser;

    public int pageNumber;

    // not stored in db - denotes the indentation level when displaying this comment
    public transient int level = 0;

    public static ReaderComment fromJson(JSONObject json, long blogId) {
        if (json == null) {
            throw new IllegalArgumentException("null json comment");
        }

        ReaderComment comment = new ReaderComment();

        comment.blogId = blogId;
        comment.commentId = json.optLong("ID");
        comment.status = JSONUtils.getString(json, "status");

        // note that content may contain html, adapter needs to handle it
        comment.text = HtmlUtils.stripScript(JSONUtils.getString(json, "content"));

        comment.published = JSONUtils.getString(json, "date");
        comment.timestamp = DateTimeUtils.iso8601ToTimestamp(comment.published);

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            comment.postId = jsonPost.optLong("ID");
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            comment.authorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
            comment.authorAvatar = JSONUtils.getString(jsonAuthor, "avatar_URL");
            comment.authorUrl = JSONUtils.getString(jsonAuthor, "URL");
            comment.authorId = jsonAuthor.optLong("ID");
            comment.authorBlogId = jsonAuthor.optLong("site_ID");
        }

        JSONObject jsonParent = json.optJSONObject("parent");
        if (jsonParent != null) {
            comment.parentId = jsonParent.optLong("ID");
        }

        // like info is found under meta/data/likes when meta=likes query param is used
        JSONObject jsonLikes = JSONUtils.getJSONChild(json, "meta/data/likes");
        if (jsonLikes != null) {
            comment.numLikes = jsonLikes.optInt("found");
            comment.isLikedByCurrentUser = JSONUtils.getBool(jsonLikes, "i_like");
        }

        return comment;
    }

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }

    public void setAuthorName(String authorName) {
        this.authorName = StringUtils.notNullStr(authorName);
    }

    public String getAuthorAvatar() {
        return StringUtils.notNullStr(authorAvatar);
    }
    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = StringUtils.notNullStr(authorAvatar);
    }

    public String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = StringUtils.notNullStr(authorUrl);
    }

    public String getText() {
        return StringUtils.notNullStr(text);
    }
    public void setText(String text) {
        this.text = StringUtils.notNullStr(text);
    }

    public String getStatus() {
        return StringUtils.notNullStr(status);
    }
    public void setStatus(String status) {
        this.status = StringUtils.notNullStr(status);
    }

    public String getPublished() {
        return StringUtils.notNullStr(published);
    }
    public void setPublished(String published) {
        this.published = StringUtils.notNullStr(published);
    }

    public boolean hasAuthorUrl() {
        return !TextUtils.isEmpty(authorUrl);
    }

    public boolean hasAuthorBlogId() {
        return (authorBlogId != 0);
    }

    public boolean hasAuthorAvatar() {
        return !TextUtils.isEmpty(authorAvatar);
    }
}
