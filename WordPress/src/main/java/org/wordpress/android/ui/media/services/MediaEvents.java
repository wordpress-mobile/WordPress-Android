package org.wordpress.android.ui.media.services;

public class MediaEvents {
    public static class MediaUploadSucceeded {
        public final String mLocalBlogId;
        public final String mLocalMediaId;
        public final String mRemoteMediaId;
        public final String mRemoteMediaUrl;
        MediaUploadSucceeded(String localBlogId, String localMediaId, String remoteMediaId, String remoteMediaUrl) {
            mLocalBlogId = localBlogId;
            mLocalMediaId = localMediaId;
            mRemoteMediaId = remoteMediaId;
            mRemoteMediaUrl = remoteMediaUrl;
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

    public static class MediaUploadProgress {
        public final String mLocalMediaId;
        public final float mProgress;
        MediaUploadProgress(String localMediaId, float progress) {
            mLocalMediaId = localMediaId;
            mProgress = progress;
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
