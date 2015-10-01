package org.wordpress.android.ui.media.services;

public class MediaUploadEvents {
    public static class MediaUploadSucceeded {
        public final String mLocalId;
        public final String mRemoteId;
        public final String mRemoteUrl;
        MediaUploadSucceeded(String localId, String remoteId, String remoteUrl) {
            mLocalId = localId;
            mRemoteId = remoteId;
            mRemoteUrl = remoteUrl;
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

    public static class MediaUploadProgress {
        public final String mLocalId;
        public final float mProgress;
        MediaUploadProgress(String localId, float progress) {
            mLocalId = localId;
            mProgress = progress;
        }
    }
}
