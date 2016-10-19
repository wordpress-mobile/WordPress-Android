package org.wordpress.android.ui.reader.models;

import java.io.Serializable;

public class ReaderBlogIdPostId implements Serializable {
    private static final long serialVersionUID = 0L;

    private final long blogId;
    private final long postId;
    private final String blogSlug;
    private final String postSlug;

    public ReaderBlogIdPostId(long blogId, long postId) {
        this.blogId = blogId;
        this.postId = postId;
        this.blogSlug = null;
        this.postSlug = null;
    }

    public ReaderBlogIdPostId(String blogSlug, String postSlug) {
        this.blogId = 0;
        this.postId = 0;

        this.blogSlug = blogSlug;
        this.postSlug = postSlug;
    }

    public long getBlogId() {
        return blogId;
    }

    public long getPostId() {
        return postId;
    }

    public String getBlogSlug() {
    return blogSlug;
}

    public String getPostSlug() {
        return postSlug;
    }
}
