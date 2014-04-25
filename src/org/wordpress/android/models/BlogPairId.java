package org.wordpress.android.models;

import java.io.Serializable;

/**
 * Simple POJO to store a pair of ids: a remoteBlogId + a genericId.
 * Could be used to identify a comment (remoteBlogId + commentId) or a post (remoteBlogId + postId)
 */
public class BlogPairId implements Serializable {
    public long mId;
    public long mRemoteBlogId;
    public BlogPairId(long remoteBlogId, long id) {
        mRemoteBlogId = remoteBlogId;
        mId = id;
    }
}