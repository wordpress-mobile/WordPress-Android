package org.wordpress.android.ui.uploads;

interface UploadHandler {
    boolean hasInProgressUploads();
    void cancelInProgressUploads();
}
