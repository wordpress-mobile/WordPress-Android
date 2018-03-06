package org.wordpress.android.ui.uploads;

import android.support.annotation.NonNull;

interface UploadHandler<T> {
    void upload(@NonNull T object);

    boolean hasInProgressUploads();

    void cancelInProgressUploads();
}
