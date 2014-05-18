package org.wordpress.android.ui.reader.models;

import java.io.Serializable;

public class ReaderBlogIdPostId implements Serializable {
    private static final long serialVersionUID = 0L;

    public long blogId;
    public long postId;

    public ReaderBlogIdPostId(long blogId, long postId) {
        this.blogId = blogId;
        this.postId = postId;
    }
}
