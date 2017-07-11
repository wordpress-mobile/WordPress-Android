package org.wordpress.android.ui.uploads;

interface UploadHandler<T> {
    void upload(T object);
    boolean hasInProgressUploads();
    void cancelInProgressUploads();
}
