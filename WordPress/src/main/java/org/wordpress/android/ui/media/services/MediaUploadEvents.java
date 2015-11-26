package org.wordpress.android.ui.media.services;

public class MediaUploadEvents {
    public static class MediaUploadSucceed {
        public final String mLocalId;
        public final String mRemoteId;
        MediaUploadSucceed(String localId, String remoteId) {
            mLocalId = localId;
            mRemoteId = remoteId;
        }
    }

    public static class MediaUploadFailed {
        public final String mLocalId;
        public final String mErrorMessage;
        MediaUploadFailed(String localId, String errorMessage) {
            mLocalId = localId;
            mErrorMessage = errorMessage;
        }
    }

    public static class MediaFetched {
        public final String mBlogId;
        public final String mMediaId;
        MediaFetched(String blogId, String mediaId) {
            mBlogId = blogId;
            mMediaId = mediaId;
        }
    }
}
