package org.wordpress.android.editor;

import java.util.Map;

public interface OnJsEditorStateChangedListener {
    void onDomLoaded();
    void onSelectionChanged(Map<String, String> selectionArgs);
    void onSelectionStyleChanged(Map<String, Boolean> changeSet);
    void onMediaTapped(String mediaId, String url, String meta, String uploadStatus);
    void onLinkTapped(String url, String title);
    void onGetHtmlResponse(Map<String, String> responseArgs);
}
