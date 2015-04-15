package org.wordpress.android.editor;

import java.util.Map;

public interface OnJsEditorStateChangedListener {
    void onDomLoaded();
    void onSelectionStyleChanged(Map<String, Boolean> changeSet);
}
