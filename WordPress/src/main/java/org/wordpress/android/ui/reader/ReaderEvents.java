package org.wordpress.android.ui.reader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.models.ReaderSimplePostList;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter;
import org.wordpress.android.util.StringUtils;

/**
 * Reader-related EventBus event classes
 */
public class ReaderEvents {
    private ReaderEvents() {
        throw new AssertionError();
    }

    public static class FollowedTagsChanged {
        private final boolean mDidSucceed;

        public FollowedTagsChanged(boolean didSucceed) {
            mDidSucceed = didSucceed;
        }
        public boolean didSucceed() {
            return mDidSucceed;
        }
    }

    public static class RecommendedTagsChanged {
    }

    public static class TagAdded {
        private final String mTagName;

        public TagAdded(String tagName) {
            mTagName = tagName;
        }

        public String getTagName() {
            return StringUtils.notNullStr(mTagName);
        }
    }

    public static class FollowedBlogsChanged {
    }

    public static class RecommendedBlogsChanged {
    }

    public static class InterestTagsFetchEnded {
        private final ReaderTagList mInterestTags;
        private final boolean mDidSucceed;

        public InterestTagsFetchEnded(ReaderTagList interestTags, boolean didSucceed) {
            mInterestTags = interestTags;
            mDidSucceed = didSucceed;
        }

        public ReaderTagList getInterestTags() {
            return mInterestTags;
        }

        public boolean didSucceed() {
            return mDidSucceed;
        }
    }

    public static class SinglePostDownloaded {
    }

    public static class UpdatePostsStarted {
        private final ReaderTag mReaderTag;
        private final ReaderPostServiceStarter.UpdateAction mAction;

        public UpdatePostsStarted(ReaderPostServiceStarter.UpdateAction action, final ReaderTag readerTag) {
            mAction = action;
            mReaderTag = readerTag;
        }

        public UpdatePostsStarted(ReaderPostServiceStarter.UpdateAction action) {
            mAction = action;
            mReaderTag = null;
        }

        public ReaderPostServiceStarter.UpdateAction getAction() {
            return mAction;
        }

        public @Nullable ReaderTag getReaderTag() {
            return mReaderTag;
        }
    }

    public static class UpdatePostsEnded {
        private final ReaderTag mReaderTag;
        private final ReaderActions.UpdateResult mResult;
        private final ReaderPostServiceStarter.UpdateAction mAction;

        public UpdatePostsEnded(ReaderActions.UpdateResult result,
                                ReaderPostServiceStarter.UpdateAction action) {
            mResult = result;
            mAction = action;
            mReaderTag = null;
        }

        public UpdatePostsEnded(ReaderTag readerTag,
                                ReaderActions.UpdateResult result,
                                ReaderPostServiceStarter.UpdateAction action) {
            mReaderTag = readerTag;
            mResult = result;
            mAction = action;
        }

        public ReaderTag getReaderTag() {
            return mReaderTag;
        }

        public ReaderActions.UpdateResult getResult() {
            return mResult;
        }

        public ReaderPostServiceStarter.UpdateAction getAction() {
            return mAction;
        }
    }

    public static class SearchPostsStarted {
        private final String mQuery;
        private final int mOffset;

        public SearchPostsStarted(@NonNull String query, int offset) {
            mQuery = query;
            mOffset = offset;
        }

        public String getQuery() {
            return mQuery;
        }

        public int getOffset() {
            return mOffset;
        }
    }

    public static class SearchPostsEnded {
        private final String mQuery;
        private final boolean mDidSucceed;
        private final int mOffset;

        public SearchPostsEnded(@NonNull String query, int offset, boolean didSucceed) {
            mQuery = query;
            mOffset = offset;
            mDidSucceed = didSucceed;
        }

        public boolean didSucceed() {
            return mDidSucceed;
        }

        public String getQuery() {
            return mQuery;
        }

        public int getOffset() {
            return mOffset;
        }
    }

    public static class UpdateCommentsStarted {
    }

    public static class UpdateCommentsEnded {
        private final ReaderActions.UpdateResult mResult;

        public UpdateCommentsEnded(ReaderActions.UpdateResult result) {
            mResult = result;
        }

        public ReaderActions.UpdateResult getResult() {
            return mResult;
        }
    }

    public static class RelatedPostsUpdated {
        private final long mSourcePostId;
        private final long mSourceSiteId;
        private final ReaderSimplePostList mLocalRelatedPosts;
        private final ReaderSimplePostList mGlobalRelatedPosts;

        public RelatedPostsUpdated(@NonNull ReaderPost sourcePost,
                                   @NonNull ReaderSimplePostList localRelatedPosts,
                                   @NonNull ReaderSimplePostList globalRelatedPosts) {
            mSourcePostId = sourcePost.postId;
            mSourceSiteId = sourcePost.blogId;
            mLocalRelatedPosts = localRelatedPosts;
            mGlobalRelatedPosts = globalRelatedPosts;
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

        public boolean hasLocalRelatedPosts() {
            return mLocalRelatedPosts.size() > 0;
        }

        public boolean hasGlobalRelatedPosts() {
            return mGlobalRelatedPosts.size() > 0;
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
