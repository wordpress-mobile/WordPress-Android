package org.wordpress.android.ui.uploads;

interface UploadHandler {
    void unregister();
    boolean hasInProgressUploads();
    void cancelInProgressUploads();
}
