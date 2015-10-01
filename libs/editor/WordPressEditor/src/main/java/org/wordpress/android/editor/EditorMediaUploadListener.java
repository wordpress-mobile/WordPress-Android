package org.wordpress.android.editor;

public interface EditorMediaUploadListener {
    void onMediaUploadSucceeded(String localId, String remoteUrl);
    void onMediaUploadProgress(String localId, float progress);
    void onMediaUploadFailed(String localId);
}
