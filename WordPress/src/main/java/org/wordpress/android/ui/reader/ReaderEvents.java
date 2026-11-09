package org.wordpress.android.ui.reader;

import androidx.annotation.NonNull;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.models.ReaderSimplePostList;

/**
 * Reader-related EventBus event classes
 */
public class ReaderEvents {
    private ReaderEvents() {
        throw new AssertionError();
    }

    public static class RelatedPostsUpdated {
        private final long mSourcePostId;
        private final long mSourceSiteId;
        private final ReaderSimplePostList mLocalRelatedPosts;
        private final ReaderSimplePostList mGlobalRelatedPosts;
        private final boolean mDidSucceed;

        public RelatedPostsUpdated(
                @NonNull ReaderPost sourcePost,
                @NonNull ReaderSimplePostList localRelatedPosts,
                @NonNull ReaderSimplePostList globalRelatedPosts,
                boolean didSucceed
        ) {
            mSourcePostId = sourcePost.postId;
            mSourceSiteId = sourcePost.blogId;
            mLocalRelatedPosts = localRelatedPosts;
            mGlobalRelatedPosts = globalRelatedPosts;
            mDidSucceed = didSucceed;
        }

        public long getSourcePostId() {
            return mSourcePostId;
        }

        public long getSourceSiteId() {
            return mSourceSiteId;
        }

        public ReaderSimplePostList getLocalRelatedPosts() {
            return mLocalRelatedPosts;
        }

        public ReaderSimplePostList getGlobalRelatedPosts() {
            return mGlobalRelatedPosts;
        }

        public boolean didSucceed() {
            return mDidSucceed;
        }
    }

    public static class PostSlugsRequestCompleted {
        private final int mStatusCode;
        private final long mBlogId;
        private final long mPostId;

        public PostSlugsRequestCompleted(int statusCode, long blogId, long postId) {
            mStatusCode = statusCode;
            mBlogId = blogId;
            mPostId = postId;
        }

        public int getStatusCode() {
            return mStatusCode;
        }

        public long getBlogId() {
            return mBlogId;
        }

        public long getPostId() {
            return mPostId;
        }
    }

    public static class DoSignIn {
    }
}
