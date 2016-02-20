package org.wordpress.android.ui.media.services;

public class MediaEvents {
    public static class MediaUploadSucceeded {
        public final String mLocalBlogId;
        public final String mLocalMediaId;
        public final String mRemoteMediaId;
        public final String mRemoteMediaUrl;
        public final String mSecondaryRemoteMediaId;
        MediaUploadSucceeded(String localBlogId, String localMediaId, String remoteMediaId, String remoteMediaUrl,
                             String secondaryRemoteMediaId) {
            mLocalBlogId = localBlogId;
            mLocalMediaId = localMediaId;
            mRemoteMediaId = remoteMediaId;
            mRemoteMediaUrl = remoteMediaUrl;
            mSecondaryRemoteMediaId = secondaryRemoteMediaId;
        }
    }

    public static class MediaUploadFailed {
        public final String mLocalMediaId;
        public final String mErrorMessage;
        public final boolean mIsGenericMessage;
        MediaUploadFailed(String localMediaId, String errorMessage, boolean isGenericMessage) {
            mLocalMediaId = localMediaId;
            mErrorMessage = errorMessage;
            mIsGenericMessage = isGenericMessage;
        }
        MediaUploadFailed(String localMediaId, String errorMessage) {
            this(localMediaId, errorMessage, false);
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
