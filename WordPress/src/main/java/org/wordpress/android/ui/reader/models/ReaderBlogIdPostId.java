package org.wordpress.android.ui.reader.models;

import java.io.Serializable;

public class ReaderBlogIdPostId implements Serializable {
    private static final long serialVersionUID = 0L;

    private final long blogId;
    private final long postId;

    public ReaderBlogIdPostId(long blogId, long postId) {
        this.blogId = blogId;
        this.postId = postId;
    }

    public long getBlogId() {
        return blogId;
    }

    public long getPostId() {
        return postId;
    }
}
