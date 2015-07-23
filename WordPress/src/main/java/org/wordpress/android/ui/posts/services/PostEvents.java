package org.wordpress.android.ui.posts.services;

import org.wordpress.android.util.StringUtils;
import org.xmlrpc.android.ApiHelper;

public class PostEvents {
    public static class PostUploadSucceed {
        public final int mLocalBlogId;
        public final String mRemotePostId;
        public final boolean mIsPage;

        PostUploadSucceed(int localBlogId, String remotePostId, boolean isPage) {
            mLocalBlogId = localBlogId;
            mRemotePostId = remotePostId;
            mIsPage = isPage;
        }
    }

    public static class PostUploadFailed {
        public final int mLocalId;

        PostUploadFailed(int localId) {
            mLocalId = localId;
        }
    }

    public static class PostMediaInfoUpdated {
        private long mMediaId;
        private String mMediaUrl;

        PostMediaInfoUpdated(long mediaId, String mediaUrl) {
            mMediaId = mediaId;
            mMediaUrl = mediaUrl;
        }
        public long getMediaId() {
            return mMediaId;
        }
        public String getMediaUrl() {
            return StringUtils.notNullStr(mMediaUrl);
        }
    }

    public static class RequestPosts {
        private int mBlogId;
        private boolean mIsPage;
        private boolean mCanLoadMore;
        private boolean mFailed;
        private ApiHelper.ErrorType mErrorType = null;

        RequestPosts(int blogId, boolean isPage) {
            mBlogId = blogId;
            mIsPage = isPage;
            mFailed = false;
        }
        public int getBlogId() {
            return mBlogId;
        }
        public boolean isPage() {
            return mIsPage;
        }
        public boolean canLoadMore() {
            return mCanLoadMore;
        }
        public void setCanLoadMore(boolean canLoadMore) {
            mCanLoadMore = canLoadMore;
        }
        public boolean getFailed() {
            return mFailed;
        }
        public ApiHelper.ErrorType getErrorType() {
            return mErrorType;
        }
        public void setErrorType(ApiHelper.ErrorType errorType) {
            mErrorType = errorType;
            mFailed = true;
        }
    }

    public static class RequestSinglePost {
        private int mBlogId;
        private String mPostId;
        private boolean mIsPage;
        private boolean mFailed;

        RequestSinglePost(int blogId, String remotePostId, boolean isPage) {
            mBlogId = blogId;
            mPostId = remotePostId;
            mIsPage = isPage;
            mFailed = false;
        }

        public int getBlogId() {
            return mBlogId;
        }
        public String getPostId() {
            return StringUtils.notNullStr(mPostId);
        }
        public boolean isPage() {
            return mIsPage;
        }
        public boolean getFailed() {
            return mFailed;
        }
        public void setFailed(boolean failed) {
            mFailed = failed;
        }
    }

}
