package org.wordpress.android.ui.media.services;

public class MediaEvents {
    public static class MediaUploadSucceed {
        public final String mLocalBlogId;
        public final String mLocalMediaId;
        public final String mRemoteMediaId;
        MediaUploadSucceed(String localBlogId, String localMediaId, String remoteMediaId) {
            mLocalBlogId = localBlogId;
            mLocalMediaId = localMediaId;
            mRemoteMediaId = remoteMediaId;
        }
    }

    public static class MediaUploadFailed {
        public final String mLocalMediaId;
        public final String mErrorMessage;
        MediaUploadFailed(String localMediaId, String errorMessage) {
            mLocalMediaId = localMediaId;
            mErrorMessage = errorMessage;
        }
    }

    public static class MediaChanged {
        public final String mLocalBlogId;
        public final String mMediaId;
        public MediaChanged(String localBlogId, String mediaId) {
            mLocalBlogId = localBlogId;
            mMediaId = mediaId;
        }
    }
}
