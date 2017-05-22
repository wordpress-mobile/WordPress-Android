package org.wordpress.android.ui.reader.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

/**
 * Information used by ReaderTagHeaderView
 */

public class ReaderTagHeaderInfo {
    private String imageUrl;
    private long sourceBlogId;
    private long sourcePostId;
    private String authorName;
    private String blogName;

    public String getImageUrl() {
        return StringUtils.notNullStr(imageUrl);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = StringUtils.notNullStr(imageUrl);
    }

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }

    public void setAuthorName(String authorName) {
        this.authorName = StringUtils.notNullStr(authorName);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }

    public void setBlogName(String blogName) {
        this.blogName = StringUtils.notNullStr(blogName);
    }

    public long getSourceBlogId() {
        return sourceBlogId;
    }

    public void setSourceBlogId(long blogId) {
        this.sourceBlogId = blogId;
    }

    public long getSourcePostId() {
        return sourcePostId;
    }

    public void setSourcePostId(long postId) {
        this.sourcePostId = postId;
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(blogName);
    }

    public boolean hasSourcePost() {
        return sourceBlogId != 0 && sourcePostId != 0;
    }
}
