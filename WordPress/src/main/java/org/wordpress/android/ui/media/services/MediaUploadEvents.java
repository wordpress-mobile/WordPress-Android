package org.wordpress.android.ui.media.services;

public class MediaUploadEvents {
    public static class MediaUploadCancelled {}

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

    public static class MediaDownloadSucceed {
        public final String mLocalId;
        MediaDownloadSucceed(String localId) {
            mLocalId = localId;
        }
    }

    public static class MediaDownloadFailed {
        public final String mLocalId;
        public final String mErrorMessage;
        MediaDownloadFailed(String localId, String errorMessage) {
            mLocalId = localId;
            mErrorMessage = errorMessage;
        }
    }

    public static class MediaUploadStarted {
        public final String mLocalId;
        MediaUploadStarted(String localId) {
            mLocalId = localId;
        }
    }
}
