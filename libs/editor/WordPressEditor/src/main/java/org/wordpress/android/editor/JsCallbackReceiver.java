package org.wordpress.android.editor;

import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wordpress.android.editor.EditorFragmentAbstract.MediaType;

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
    private static final String CALLBACK_VIDEO_REPLACED = "callback-video-replaced";
    private static final String CALLBACK_IMAGE_TAP = "callback-image-tap";
    private static final String CALLBACK_LINK_TAP = "callback-link-tap";
    private static final String CALLBACK_MEDIA_REMOVED = "callback-media-removed";

    private static final String CALLBACK_VIDEOPRESS_INFO_REQUEST = "callback-videopress-info-request";

    private static final String CALLBACK_LOG = "callback-log";

    private static final String CALLBACK_RESPONSE_STRING = "callback-response-string";

    private static final String CALLBACK_ACTION_FINISHED = "callback-action-finished";

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
                Set<String> rawStyleSet = Utils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);

                // Strip link details from active style set
                Set<String> newStyleSet = new HashSet<>();
                for (String element : rawStyleSet) {
                    if (element.matches("link:(.*)")) {
                        newStyleSet.add("link");
                    } else if (!element.matches("link-title:(.*)")) {
                        newStyleSet.add(element);
                    }
                }

                mListener.onSelectionStyleChanged(Utils.getChangeMapFromSets(mPreviousStyleSet, newStyleSet));
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
                AppLog.d(AppLog.T.EDITOR, "Image replaced, " + params);

                // Extract the local media id from the callback string (stripping the 'id=' part)
                if (params.length() > 3) {
                    mListener.onMediaReplaced(params.substring(3));
                }
                break;
            case CALLBACK_VIDEO_REPLACED:
                AppLog.d(AppLog.T.EDITOR, "Video replaced, " + params);

                // Extract the local media id from the callback string (stripping the 'id=' part)
                if (params.length() > 3) {
                    mListener.onMediaReplaced(params.substring(3));
                }
                break;
            case CALLBACK_IMAGE_TAP:
                AppLog.d(AppLog.T.EDITOR, "Image tapped, " + params);

                String uploadStatus = "";

                List<String> mediaIds = new ArrayList<>();
                mediaIds.add("id");
                mediaIds.add("url");
                mediaIds.add("meta");
                mediaIds.add("type");

                Set<String> mediaDataSet = Utils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, mediaIds);
                Map<String, String> mediaDataMap = Utils.buildMapFromKeyValuePairs(mediaDataSet);

                String mediaId = mediaDataMap.get("id");

                String mediaUrl = mediaDataMap.get("url");
                if (mediaUrl != null) {
                    mediaUrl = Utils.decodeHtml(mediaUrl);
                }

                MediaType mediaType = MediaType.fromString(mediaDataMap.get("type"));

                String mediaMeta = mediaDataMap.get("meta");
                JSONObject mediaMetaJson = new JSONObject();

                if (mediaMeta != null) {
                    mediaMeta = Utils.decodeHtml(mediaMeta);

                    try {
                        mediaMetaJson = new JSONObject(mediaMeta);
                        String classes = JSONUtils.getString(mediaMetaJson, "classes");
                        Set<String> classesSet = Utils.splitDelimitedString(classes, ", ");

                        if (classesSet.contains("uploading")) {
                            uploadStatus = "uploading";
                        } else if (classesSet.contains("failed")) {
                            uploadStatus = "failed";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        AppLog.d(AppLog.T.EDITOR, "Media meta data from callback-image-tap was not JSON-formatted");
                    }
                }

                mListener.onMediaTapped(mediaId, mediaType, mediaMetaJson, uploadStatus);
                break;
            case CALLBACK_LINK_TAP:
                // Extract and HTML-decode the link data from the callback params
                AppLog.d(AppLog.T.EDITOR, "Link tapped, " + params);

                List<String> linkIds = new ArrayList<>();
                linkIds.add("url");
                linkIds.add("title");

                Set<String> linkDataSet = Utils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, linkIds);
                Map<String, String> linkDataMap = Utils.buildMapFromKeyValuePairs(linkDataSet);

                String url = linkDataMap.get("url");
                if (url != null) {
                    url = Utils.decodeHtml(url);
                }

                String title = linkDataMap.get("title");
                if (title != null) {
                    title = Utils.decodeHtml(title);
                }

                mListener.onLinkTapped(url, title);
                break;
            case CALLBACK_MEDIA_REMOVED:
                AppLog.d(AppLog.T.EDITOR, "Media removed, " + params);
                // Extract the media id from the callback string (stripping the 'id=' part of the callback string)
                if (params.length() > 3) {
                    mListener.onMediaRemoved(params.substring(3));
                }
                break;
            case CALLBACK_VIDEOPRESS_INFO_REQUEST:
                // Extract the VideoPress id from the callback string (stripping the 'id=' part of the callback string)
                if (params.length() > 3) {
                    mListener.onVideoPressInfoRequested(params.substring(3));
                }
                break;
            case CALLBACK_LOG:
                // Strip 'msg=' from beginning of string
                if (params.length() > 4) {
                    AppLog.d(AppLog.T.EDITOR, callbackId + ": " + params.substring(4));
                }
                break;
            case CALLBACK_RESPONSE_STRING:
                AppLog.d(AppLog.T.EDITOR, callbackId + ": " + params);
                Set<String> responseDataSet;
                if (params.startsWith("function=") && params.contains(JS_CALLBACK_DELIMITER)) {
                    String functionName = params.substring("function=".length(), params.indexOf(JS_CALLBACK_DELIMITER));

                    List<String> responseIds = new ArrayList<>();
                    switch (functionName) {
                        case "getHTMLForCallback":
                            responseIds.add("id");
                            responseIds.add("contents");
                            break;
                        case "getSelectedTextToLinkify":
                            responseIds.add("result");
                            break;
                        case "getFailedMedia":
                            responseIds.add("ids");
                    }

                    responseDataSet = Utils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, responseIds);
                } else {
                    responseDataSet = Utils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                }
                mListener.onGetHtmlResponse(Utils.buildMapFromKeyValuePairs(responseDataSet));
                break;
            case CALLBACK_ACTION_FINISHED:
                mListener.onActionFinished();
                break;
            default:
                AppLog.d(AppLog.T.EDITOR, "Unhandled callback: " + callbackId + ":" + params);
        }
    }
}
