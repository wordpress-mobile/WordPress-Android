package org.wordpress.android.editor;

import org.wordpress.android.util.helpers.MediaFile;

public interface EditorMediaUploadListener {
    void onMediaUploadReattached(String localId, float currentProgress);
    void onMediaUploadSucceeded(String localId, MediaFile mediaFile);
    void onMediaUploadProgress(String localId, float progress);
    void onMediaUploadFailed(String localId, EditorFragmentAbstract.MediaType mediaType, String errorMessage);
    void onGalleryMediaUploadSucceeded(long galleryId, long remoteId, int remaining);
}
