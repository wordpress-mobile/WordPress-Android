package org.wordpress.android.editor;

public interface EditorImageSettingsListener {
    void onImageSettingsRequested(EditorImageMetaData editorImageMetaData);
    void onAudioSettingsRequested(String mediaModelId);
}
