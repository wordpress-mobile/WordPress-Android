package org.wordpress.android.editor;

import android.webkit.JavascriptInterface;

import org.wordpress.android.util.AppLog;

import java.util.HashSet;
import java.util.Set;

public class JsCallbackHandler {
    private static final String JS_CALLBACK_DELIMITER = "~";

    private static final String CALLBACK_LOG = "callback-log";

    private static final String CALLBACK_DOM_LOADED = "callback-dom-loaded";

    private static final String CALLBACK_SELECTION_STYLE = "callback-selection-style";
    private static final String CALLBACK_SELECTION_CHANGED = "callback-selection-changed";

    private static final String CALLBACK_FOCUS_IN = "callback-focus-in";
    private static final String CALLBACK_FOCUS_OUT = "callback-focus-out";

    private static final String CALLBACK_IMAGE_REPLACED = "callback-image-replaced";
    private static final String CALLBACK_IMAGE_TAP = "callback-image-tap";
    private static final String CALLBACK_INPUT = "callback-input";
    private static final String CALLBACK_LINK_TAP = "callback-link-tap";

    private static final String CALLBACK_NEW_FIELD = "callback-new-field";

    private final JsCallbackListener mJsCallbackListener;

    private Set<String> mPreviousStyleSet = new HashSet<>();

    public JsCallbackHandler(EditorFragmentAbstract editorFragmentAbstract) {
        mJsCallbackListener = (JsCallbackListener) editorFragmentAbstract;
    }

    @JavascriptInterface
    public void executeCallback(String callbackId, String params) {
        switch (callbackId) {
            case CALLBACK_DOM_LOADED:
                mJsCallbackListener.onDomLoaded();
                break;
            case CALLBACK_SELECTION_STYLE:
                // Compare the new styles to the previous ones, and notify the JsCallbackListener of the changeset
                Set<String> newStyleSet = Utils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                mJsCallbackListener.onSelectionStyleChanged(Utils.getChangeMapFromSets(mPreviousStyleSet, newStyleSet));
                mPreviousStyleSet = newStyleSet;
                break;
            case CALLBACK_LOG:
                // Strip 'msg=' from beginning of string
                AppLog.d(AppLog.T.EDITOR, callbackId + ": " + params.substring(4));
                break;
            default:
                AppLog.d(AppLog.T.EDITOR, "unhandled callback: " + callbackId + ":" + params);
        }
    }
}
