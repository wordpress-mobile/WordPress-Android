package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderPost;

/**
 * simplified version of a reader post that contains only the fields necessary for a related post
 */
public class ReaderRelatedPost {
    private final long mPostId;
    private final long mBlogId;
    private final String mTitle;
    private final String mByline;
    private final String mFeaturedImage;

    public ReaderRelatedPost(@NonNull ReaderPost post) {
        mPostId = post.postId;
        mBlogId = post.blogId;

        mTitle = post.getTitle();
        mFeaturedImage = post.getFeaturedImage();

        if (post.hasAuthorName() && post.hasBlogName()) {
            if (post.getAuthorName().equalsIgnoreCase(post.getBlogName())) {
                mByline = post.getBlogName();
            } else {
                mByline = post.getAuthorName() + ", " + post.getBlogName();
            }
        } else if (post.hasAuthorName()) {
            mByline = post.getAuthorName();
        } else if (post.hasBlogName()) {
            mByline = post.getBlogName();
        } else {
            mByline = "";
        }
    }

    public long getPostId() {
        return mPostId;
    }

    public long getBlogId() {
        return mBlogId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getByline() {
        return mByline;
    }

    public String getFeaturedImage() {
        return mFeaturedImage;
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(mFeaturedImage);
    }
}
