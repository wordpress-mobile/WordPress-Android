package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.UrlUtils;

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
         * will have a name, but in rare cases there isn't one so we show the domain instead
         */
        String blogNameOrDomain;
        boolean hasBlogNameOrDomain;
        if (post.hasBlogName()) {
            blogNameOrDomain = post.getBlogName();
            hasBlogNameOrDomain = true;
        } else if (post.hasBlogUrl()) {
            blogNameOrDomain = UrlUtils.getHost(post.getBlogUrl());
            hasBlogNameOrDomain = true;
        } else {
            blogNameOrDomain = null;
            hasBlogNameOrDomain = false;
        }

        /*
         * The byline should show the author name and blog name if both are available, but if
         * they're the same (which happens frequently) we only need to show the blog name.
         * Otherwise, show either the blog name or author name depending on which is available.
         */
        if (post.hasAuthorName() && hasBlogNameOrDomain) {
            if (post.getAuthorName().equalsIgnoreCase(blogNameOrDomain)) {
                mByline = blogNameOrDomain;
            } else {
                mByline = post.getAuthorName() + ", " + blogNameOrDomain;
            }
        } else if (post.hasAuthorName()) {
            mByline = post.getAuthorName();
        } else if (hasBlogNameOrDomain) {
            mByline = blogNameOrDomain;
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
