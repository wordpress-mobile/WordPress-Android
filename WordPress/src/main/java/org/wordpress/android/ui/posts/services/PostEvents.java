package org.wordpress.android.ui.posts.services;

import org.wordpress.android.util.StringUtils;
import org.xmlrpc.android.ApiHelper;

public class PostEvents {

    public static class PostUploadStarted {
        public final int mLocalBlogId;

        PostUploadStarted(int localBlogId) {
            mLocalBlogId = localBlogId;
        }
    }

    public static class PostUploadEnded {
        public final int mLocalBlogId;
        public final boolean mSucceeded;

        PostUploadEnded(boolean succeeded, int localBlogId) {
            mSucceeded = succeeded;
            mLocalBlogId = localBlogId;
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
        private final int mBlogId;
        private final boolean mIsPage;
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

}
