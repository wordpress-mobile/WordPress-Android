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

        /*
         * we want to include the blog name in the byline when it's available, and most sites
         * will have a blog name, but in rare cases there isn't one so we show the URL instead
         */
        String blogNameOrUrl;
        boolean hasBlogNameOrUrl;
        if (post.hasBlogName()) {
            blogNameOrUrl = post.getBlogName();
            hasBlogNameOrUrl = true;
        } else if (post.hasBlogUrl()) {
            blogNameOrUrl = post.getBlogUrl();
            hasBlogNameOrUrl = true;
        } else {
            blogNameOrUrl = null;
            hasBlogNameOrUrl = false;
        }

        /*
         * The byline should show the author name and blog name if both are available, but if
         * they're the same (which happens frequently) we only need to show the blog name.
         * Otherwise, show either the blog name or author name depending on which is available.
         */
        if (post.hasAuthorName() && hasBlogNameOrUrl) {
            if (post.getAuthorName().equalsIgnoreCase(blogNameOrUrl)) {
                mByline = blogNameOrUrl;
            } else {
                mByline = post.getAuthorName() + ", " + blogNameOrUrl;
            }
        } else if (post.hasAuthorName()) {
            mByline = post.getAuthorName();
        } else if (hasBlogNameOrUrl) {
            mByline = blogNameOrUrl;
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
