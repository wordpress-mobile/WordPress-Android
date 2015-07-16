package org.wordpress.android.ui.posts.services;

import org.wordpress.android.util.StringUtils;

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

    public static class PostMediaDownloaded {
        private long mMediaId;
        private String mMediaUrl;

        PostMediaDownloaded(long mediaId, String mediaUrl) {
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
}
