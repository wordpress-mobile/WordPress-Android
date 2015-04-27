package org.wordpress.android.editor;

import android.webkit.JavascriptInterface;

import org.wordpress.android.util.AppLog;

import java.util.HashSet;
import java.util.Set;

public class JsCallbackReceiver {
    private static final String JS_CALLBACK_DELIMITER = "~";

    private static final String CALLBACK_DOM_LOADED = "callback-dom-loaded";
    private static final String CALLBACK_NEW_FIELD = "callback-new-field";

    private static final String CALLBACK_INPUT = "callback-input";
    private static final String CALLBACK_SELECTION_CHANGED = "callback-selection-changed";
    private static final String CALLBACK_SELECTION_STYLE = "callback-selection-style";

    private static final String CALLBACK_FOCUS_IN = "callback-focus-in";
    private static final String CALLBACK_FOCUS_OUT = "callback-focus-out";

    private static final String CALLBACK_IMAGE_REPLACED = "callback-image-replaced";
    private static final String CALLBACK_IMAGE_TAP = "callback-image-tap";
    private static final String CALLBACK_LINK_TAP = "callback-link-tap";

    private static final String CALLBACK_LOG = "callback-log";

    private final OnJsEditorStateChangedListener mListener;

    private Set<String> mPreviousStyleSet = new HashSet<>();

    public JsCallbackReceiver(EditorFragmentAbstract editorFragmentAbstract) {
        mListener = (OnJsEditorStateChangedListener) editorFragmentAbstract;
    }

    @JavascriptInterface
    public void executeCallback(String callbackId, String params) {
        switch (callbackId) {
            case CALLBACK_DOM_LOADED:
                mListener.onDomLoaded();
                break;
            case CALLBACK_SELECTION_STYLE:
                // Compare the new styles to the previous ones, and notify the JsCallbackListener of the changeset
                Set<String> newStyleSet = Utils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                mListener.onSelectionStyleChanged(Utils.getChangeMapFromSets(mPreviousStyleSet,
                        newStyleSet));
                mPreviousStyleSet = newStyleSet;
                break;
            case CALLBACK_SELECTION_CHANGED:
                // Called for changes to the field in current focus and for changes made to selection
                // (includes moving the caret without selecting text)
                // TODO: Possibly needed for handling WebView scrolling when caret moves (from iOS)
                Set<String> selectionKeyValueSet = Utils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                mListener.onSelectionChanged(Utils.buildMapFromKeyValuePairs(selectionKeyValueSet));
                break;
            case CALLBACK_INPUT:
                // Called on key press
                // TODO: Possibly needed for handling WebView scrolling when caret moves (from iOS)
                break;
            case CALLBACK_FOCUS_IN:
                // TODO: Needed to handle displaying/graying the format bar when focus changes between the title and content
                AppLog.d(AppLog.T.EDITOR, "Focus in callback received");
                break;
            case CALLBACK_FOCUS_OUT:
                // TODO: Needed to handle displaying/graying the format bar when focus changes between the title and content
                AppLog.d(AppLog.T.EDITOR, "Focus out callback received");
                break;
            case CALLBACK_NEW_FIELD:
                // TODO: Used for logging/testing purposes on iOS
                AppLog.d(AppLog.T.EDITOR, "New field created, " + params);
                break;
            case CALLBACK_IMAGE_REPLACED:
                // TODO: Notifies that image upload has finished and that the local url was replaced by the remote url in the ZSS editor
                AppLog.d(AppLog.T.EDITOR, "Image replaced, " + params);
                break;
            case CALLBACK_IMAGE_TAP:
                // TODO: Notifies that an image was tapped
                AppLog.d(AppLog.T.EDITOR, "Image tapped, " + params);
                break;
            case CALLBACK_LINK_TAP:
                // TODO: Notifies that a link was tapped
                AppLog.d(AppLog.T.EDITOR, "Link tapped, " + params);
                break;
            case CALLBACK_LOG:
                // Strip 'msg=' from beginning of string
                AppLog.d(AppLog.T.EDITOR, callbackId + ": " + params.substring(4));
                break;
            default:
                AppLog.d(AppLog.T.EDITOR, "Unhandled callback: " + callbackId + ":" + params);
        }
    }
}
