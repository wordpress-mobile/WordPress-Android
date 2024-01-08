package org.wordpress.android.ui.reader.models;

import android.text.TextUtils;

import org.wordpress.android.util.StringUtils;

/**
 * Information used by ReaderTagHeaderView
 */

public class ReaderTagHeaderInfo {
    private String mImageUrl;
    private long mSourceBlogId;
    private long mSourcePostId;
    private String mAuthorName;
    private String mBlogName;

    public String getImageUrl() {
        return StringUtils.notNullStr(mImageUrl);
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = StringUtils.notNullStr(imageUrl);
    }

    public String getAuthorName() {
        return StringUtils.notNullStr(mAuthorName);
    }

    public void setAuthorName(String authorName) {
        mAuthorName = StringUtils.notNullStr(authorName);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(mBlogName);
    }

    public void setBlogName(String blogName) {
        mBlogName = StringUtils.notNullStr(blogName);
    }

    public long getSourceBlogId() {
        return mSourceBlogId;
    }

    public void setSourceBlogId(long blogId) {
        mSourceBlogId = blogId;
    }

    public long getSourcePostId() {
        return mSourcePostId;
    }

    public void setSourcePostId(long postId) {
        mSourcePostId = postId;
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(mAuthorName);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(mBlogName);
    }

    public boolean hasSourcePost() {
        return mSourceBlogId != 0 && mSourcePostId != 0;
    }
}
