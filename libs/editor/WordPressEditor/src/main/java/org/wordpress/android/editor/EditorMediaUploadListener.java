package org.wordpress.android.editor;

import org.wordpress.android.util.helpers.MediaFile;

public interface EditorMediaUploadListener {
    void onMediaUploadSucceeded(String localId, MediaFile mediaFile);
    void onMediaUploadProgress(String localId, float progress);
    void onMediaUploadFailed(String localId);
    void onGalleryMediaUploadSucceeded(long galleryId, String remoteId, int remaining);
}
