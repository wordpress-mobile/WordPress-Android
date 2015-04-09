package org.wordpress.android.editor;

import java.util.Map;

public interface JsCallbackListener {
    void onDomLoaded();
    void onSelectionStyleChanged(Map<String, Boolean> changeSet);
}
