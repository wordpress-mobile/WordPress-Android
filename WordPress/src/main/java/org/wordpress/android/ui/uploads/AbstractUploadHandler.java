package org.wordpress.android.ui.uploads;

abstract class AbstractUploadHandler {
    abstract void unregister();
    abstract boolean hasInProgressUploads();
    abstract void cancelInProgressUploads();
}
