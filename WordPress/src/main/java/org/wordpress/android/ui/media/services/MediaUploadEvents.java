package org.wordpress.android.ui.media.services;

public class MediaUploadEvents {
    public static class MediaUploadSucceed {
        public final String mLocalId;
        public final String mRemoteId;
        public final String mRemoteUrl;
        MediaUploadSucceed(String localId, String remoteId, String remoteUrl) {
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
}
