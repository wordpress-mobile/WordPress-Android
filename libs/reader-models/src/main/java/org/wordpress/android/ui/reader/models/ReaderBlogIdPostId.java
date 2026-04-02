package org.wordpress.android.ui.reader.models;

import java.io.Serializable;

public class ReaderBlogIdPostId implements Serializable {
    private static final long serialVersionUID = 0L;

    private final long mBlogId;
    private final long mPostId;

    public ReaderBlogIdPostId(long blogId, long postId) {
        mBlogId = blogId;
        mPostId = postId;
    }

    public long getBlogId() {
        return mBlogId;
    }

    public long getPostId() {
        return mPostId;
    }
}
