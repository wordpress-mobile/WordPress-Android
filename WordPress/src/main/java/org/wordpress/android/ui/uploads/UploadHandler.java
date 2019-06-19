package org.wordpress.android.ui.uploads;

import androidx.annotation.NonNull;

interface UploadHandler<T> {
    void upload(@NonNull T object);

    boolean hasInProgressUploads();

    void cancelInProgressUploads();
}
