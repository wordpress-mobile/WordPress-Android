package org.wordpress.android.ui.uploads;

abstract class AbstractUploadManager {
    abstract void unregister();
    abstract boolean hasInProgressUploads();
    abstract void cancelInProgressUploads();
}
