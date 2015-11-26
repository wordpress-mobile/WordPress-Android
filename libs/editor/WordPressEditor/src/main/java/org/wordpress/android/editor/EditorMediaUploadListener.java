package org.wordpress.android.editor;

public interface EditorMediaUploadListener {
    void onMediaUploadSucceeded(String localId, String remoteId, String remoteUrl);
    void onMediaUploadProgress(String localId, float progress);
    void onMediaUploadFailed(String localId);
    void onGalleryMediaUploadSucceeded(long galleryId, String remoteId, int remaining);
}
