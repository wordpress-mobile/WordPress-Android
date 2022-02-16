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

    private String mAuthorName;
    private String mAuthorAvatar;
    private String mAuthorUrl;
    private String mStatus;
    private String mText;
    private String mShortUrl;
    private String mAuthorEmail;

    private String mPublished;
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
        comment.mStatus = JSONUtils.getString(json, "status");
        comment.mShortUrl = JSONUtils.getString(json, "short_URL");

        // note that content may contain html, adapter needs to handle it
        comment.mText = HtmlUtils.stripScript(JSONUtils.getString(json, "content"));

        comment.mPublished = JSONUtils.getString(json, "date");
        comment.timestamp = DateTimeUtils.timestampFromIso8601(comment.mPublished);

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            comment.postId = jsonPost.optLong("ID");
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor != null) {
            // author names may contain html entities (esp. pingbacks)
            comment.mAuthorName = JSONUtils.getStringDecoded(jsonAuthor, "name");
            comment.mAuthorAvatar = JSONUtils.getString(jsonAuthor, "avatar_URL");
            comment.mAuthorUrl = JSONUtils.getString(jsonAuthor, "URL");
            comment.authorId = jsonAuthor.optLong("ID");
            comment.authorBlogId = jsonAuthor.optLong("site_ID");
            comment.mAuthorEmail = JSONUtils.getString(jsonAuthor, "email");
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
        return StringUtils.notNullStr(mAuthorName);
    }

    public void setAuthorName(String authorName) {
        this.mAuthorName = StringUtils.notNullStr(authorName);
    }

    public String getAuthorAvatar() {
        return StringUtils.notNullStr(mAuthorAvatar);
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.mAuthorAvatar = StringUtils.notNullStr(authorAvatar);
    }

    public String getAuthorUrl() {
        return StringUtils.notNullStr(mAuthorUrl);
    }

    public void setAuthorUrl(String authorUrl) {
        this.mAuthorUrl = StringUtils.notNullStr(authorUrl);
    }

    public String getText() {
        return StringUtils.notNullStr(mText);
    }

    public void setText(String text) {
        this.mText = StringUtils.notNullStr(text);
    }

    public String getStatus() {
        return StringUtils.notNullStr(mStatus);
    }

    public void setStatus(String status) {
        this.mStatus = StringUtils.notNullStr(status);
    }

    public String getPublished() {
        return StringUtils.notNullStr(mPublished);
    }

    public void setPublished(String published) {
        this.mPublished = StringUtils.notNullStr(published);
    }

    public boolean hasAuthorUrl() {
        return !TextUtils.isEmpty(mAuthorUrl);
    }

    public boolean hasAuthorBlogId() {
        return (authorBlogId != 0);
    }

    public boolean hasAuthorAvatar() {
        return !TextUtils.isEmpty(mAuthorAvatar);
    }

    public String getShortUrl() {
        return StringUtils.notNullStr(mShortUrl);
    }

    public void setShortUrl(String shortUrl) {
        mShortUrl = shortUrl;
    }

    public String getAuthorEmail() {
        return StringUtils.notNullStr(mAuthorName);
    }

    public void setAuthorEmail(String authorEmail) {
        mAuthorEmail = StringUtils.notNullStr(authorEmail);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        ReaderComment otherComment = (ReaderComment) other;
        return commentId == otherComment.commentId
               && blogId == otherComment.blogId
               && postId == otherComment.postId
               && parentId == otherComment.parentId
               && StringUtils.equals(mAuthorName, otherComment.mAuthorName)
               && StringUtils.equals(mAuthorAvatar, otherComment.mAuthorAvatar)
               && StringUtils.equals(mAuthorUrl, otherComment.mAuthorUrl)
               && StringUtils.equals(mAuthorEmail, otherComment.mAuthorEmail)
               && StringUtils.equals(mStatus, otherComment.mStatus)
               && StringUtils.equals(mText, otherComment.mText)
               && StringUtils.equals(mPublished, otherComment.mPublished)
               && StringUtils.equals(mShortUrl, otherComment.mShortUrl)
               && timestamp == otherComment.timestamp
               && authorId == otherComment.authorId
               && authorBlogId == otherComment.authorBlogId
               && numLikes == otherComment.numLikes
               && isLikedByCurrentUser == otherComment.isLikedByCurrentUser
               && pageNumber == otherComment.pageNumber;
    }
}
