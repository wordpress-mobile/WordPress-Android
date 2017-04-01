package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.editor.EditorWebViewAbstract.ErrorListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ShortcodeUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EditorFragment extends EditorFragmentAbstract implements View.OnClickListener, View.OnTouchListener,
        OnJsEditorStateChangedListener, OnImeBackListener, EditorWebViewAbstract.AuthHeaderRequestListener,
        EditorMediaUploadListener {

    public class IllegalEditorStateException extends Exception {

    }

    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";

    private static final String TAG_FORMAT_BAR_BUTTON_MEDIA = "media";
    private static final String TAG_FORMAT_BAR_BUTTON_LINK = "link";

    private static final float TOOLBAR_ALPHA_ENABLED = 1;
    private static final float TOOLBAR_ALPHA_DISABLED = 0.5f;

    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_TEXT = Arrays.asList(ClipDescription.MIMETYPE_TEXT_PLAIN,
            ClipDescription.MIMETYPE_TEXT_HTML);
    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE = Arrays.asList("image/jpeg", "image/png");

    public static final int MAX_ACTION_TIME_MS = 2000;

    private String mTitle = "";
    private String mContentHtml = "";

    private EditorWebViewAbstract mWebView;
    private View mSourceView;
    private SourceViewEditText mSourceViewTitle;
    private SourceViewEditText mSourceViewContent;

    private int mSelectionStart;
    private int mSelectionEnd;

    private String mFocusedFieldId;

    private String mTitlePlaceholder = "";
    private String mContentPlaceholder = "";

    private boolean mDomHasLoaded = false;
    private boolean mKeyboardOpenEh = false;
    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;
    private boolean mFormatBarDisabledEh = false;

    private ConcurrentHashMap<String, MediaFile> mWaitingMediaFiles = new ConcurrentHashMap<>();
    private Set<MediaGallery> mWaitingGalleries = Collections.newSetFromMap(new ConcurrentHashMap<MediaGallery, Boolean>());
    private Map<String, MediaType> mUploadingMedia = new HashMap<>();
    private Set<String> mFailedMediaIds = new HashSet<>();

    private MediaGallery mUploadingMediaGallery;

    private String mJavaScriptResult = "";

    private CountDownLatch mGetTitleCountDownLatch;
    private CountDownLatch mGetContentCountDownLatch;
    private CountDownLatch mGetSelectedTextCountDownLatch;

    private final Map<String, ToggleButton> mTagToggleButtonMap = new HashMap<>();

    private long mActionStartedAt = -1;

    private final View.OnDragListener mOnDragListener = new View.OnDragListener() {
        private long lastSetCoordsTimestamp;

        private boolean supportedEh(ClipDescription clipDescription, List<String> mimeTypesToCheck) {
            if (clipDescription == null) {
                return false;
            }

            for (String supportedMimeType : mimeTypesToCheck) {
                if (clipDescription.hasMimeType(supportedMimeType)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return supportedEh(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_TEXT) ||
                            supportedEh(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE);
                case DragEvent.ACTION_DRAG_ENTERED:
                    // would be nice to start marking the place the item will drop
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    int x = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getX());
                    int y = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getY());

                    // don't call into JS too often
                    long currentTimestamp = SystemClock.uptimeMillis();
                    if ((currentTimestamp - lastSetCoordsTimestamp) > 150) {
                        lastSetCoordsTimestamp = currentTimestamp;

                        mWebView.execJavaScriptFromString("ZSSEditor.moveCaretToCoords(" + x + ", " + y + ");");
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // clear any drop marking maybe
                    break;
                case DragEvent.ACTION_DROP:
                    if (mSourceView.getVisibility() == View.VISIBLE) {
                        if (supportedEh(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE)) {
                            // don't allow dropping images into the HTML source
                            ToastUtils.showToast(getActivity(), R.string.editor_dropped_html_images_not_allowed,
                                    ToastUtils.Duration.LONG);
                            return true;
                        } else {
                            // let the system handle the text drop
                            return false;
                        }
                    }

                    if (supportedEh(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE) &&
                            ("zss_field_title".equals(mFocusedFieldId))) {
                        // don't allow dropping images into the title field
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_title_images_not_allowed,
                                ToastUtils.Duration.LONG);
                        return true;
                    }

                    if (isAdded()) {
                        mEditorDragAndDropListener.onRequestDragAndDropPermissions(dragEvent);
                    }

                    ClipDescription clipDescription = dragEvent.getClipDescription();
                    if (clipDescription.getMimeTypeCount() < 1) {
                        break;
                    }

                    ContentResolver contentResolver = getActivity().getContentResolver();
                    ArrayList<Uri> uris = new ArrayList<>();
                    boolean unsupportedDropsFound = false;

                    for (int i = 0; i < dragEvent.getClipData().getItemCount(); i++) {
                        ClipData.Item item = dragEvent.getClipData().getItemAt(i);
                        Uri uri = item.getUri();

                        final String uriType = uri != null ? contentResolver.getType(uri) : null;
                        if (uriType != null && DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE.contains(uriType)) {
                            uris.add(uri);
                            continue;
                        } else if (item.getText() != null) {
                            insertTextToEditor(item.getText().toString());
                            continue;
                        } else if (item.getHtmlText() != null) {
                            insertTextToEditor(item.getHtmlText());
                            continue;
                        }

                        // any other drop types are not supported, including web URLs. We cannot proactively
                        // determine their mime type for filtering
                        unsupportedDropsFound = true;
                    }

                    if (unsupportedDropsFound) {
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_unsupported_files, ToastUtils
                                .Duration.LONG);
                    }

                    if (uris.size() > 0) {
                        mEditorDragAndDropListener.onMediaDropped(uris);
                    }

                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // clear any drop marking maybe
                default:
                    break;
            }
            return true;
        }

        private void insertTextToEditor(String text) {
            if (text != null) {
                mWebView.execJavaScriptFromString("ZSSEditor.insertText('" + Utils.escapeHtml(text) + "', true);");
            } else {
                ToastUtils.showToast(getActivity(), R.string.editor_dropped_text_error, ToastUtils.Duration.SHORT);
                AppLog.d(T.EDITOR, "Dropped text was null!");
            }
        }
    };

    public static EditorFragment newInstance(String title, String content) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    public EditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);

        // Setup hiding the action bar when the soft keyboard is displayed for narrow viewports
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mHideActionBarOnSoftKeyboardUp = true;
        }

        // -- WebView configuration

        mWebView = (EditorWebViewAbstract) view.findViewById(R.id.webview);

        // Revert to compatibility WebView for custom ROMs using a 4.3 WebView in Android 4.4
        if (mWebView.shouldSwitchToCompatibilityMode()) {
            ViewGroup parent = (ViewGroup) mWebView.getParent();
            int index = parent.indexOfChild(mWebView);
            parent.removeView(mWebView);
            mWebView = new EditorWebViewCompatibility(getActivity(), null);
            mWebView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            parent.addView(mWebView, index);
        }

        mWebView.setOnTouchListener(this);
        mWebView.setOnImeBackListener(this);
        mWebView.setAuthHeaderRequestListener(this);

        mWebView.setOnDragListener(mOnDragListener);

        if (mCustomHttpHeaders != null && mCustomHttpHeaders.size() > 0) {
            for (Map.Entry<String, String> entry : mCustomHttpHeaders.entrySet()) {
                mWebView.setCustomHeader(entry.getKey(), entry.getValue());
            }
        }

        // Ensure that the content field is always filling the remaining screen space
        mWebView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.execJavaScriptFromString("try {ZSSEditor.refreshVisibleViewportSize();} catch (e) " +
                                "{console.log(e)}");
                    }
                });
            }
        });

        mEditorFragmentListener.onEditorFragmentInitialized();

        initJsEditor();

        if (savedInstanceState != null) {
            setTitle(savedInstanceState.getCharSequence(KEY_TITLE));
            setContent(savedInstanceState.getCharSequence(KEY_CONTENT));
        }

        // -- HTML mode configuration

        mSourceView = view.findViewById(R.id.sourceview);
        mSourceViewTitle = (SourceViewEditText) view.findViewById(R.id.sourceview_title);
        mSourceViewContent = (SourceViewEditText) view.findViewById(R.id.sourceview_content);

        // Toggle format bar on/off as user changes focus between title and content in HTML mode
        mSourceViewTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean focusEh) {
                updateFormatBarEnabledState(!focusEh);
            }
        });

        mSourceViewTitle.setOnTouchListener(this);
        mSourceViewContent.setOnTouchListener(this);

        mSourceViewTitle.setOnImeBackListener(this);
        mSourceViewContent.setOnImeBackListener(this);

        mSourceViewContent.addTextChangedListener(new HtmlStyleTextWatcher());

        mSourceViewTitle.setHint(mTitlePlaceholder);
        mSourceViewContent.setHint("<p>" + mContentPlaceholder + "</p>");

        // attach drag-and-drop handler
        mSourceViewTitle.setOnDragListener(mOnDragListener);
        mSourceViewContent.setOnDragListener(mOnDragListener);

        // -- Format bar configuration

        setupFormatBarButtonMap(view);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditorWasPaused = true;
        mKeyboardOpenEh = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the editor was previously paused and the current orientation is landscape,
        // hide the actionbar because the keyboard is going to appear (even if it was hidden
        // prior to being paused).
        if (mEditorWasPaused
                && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mKeyboardOpenEh = true;
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditorDragAndDropListener = (EditorDragAndDropListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorDragAndDropListener");
        }
    }

    @Override
    public void onDetach() {
        // Soft cancel (delete flag off) all media uploads currently in progress
        for (String mediaId : mUploadingMedia.keySet()) {
            mEditorFragmentListener.onMediaUploadCancelClicked(mediaId, false);
        }
        super.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (mDomHasLoaded) {
            mWebView.notifyVisibilityChanged(isVisibleToUser);
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        try {
            outState.putCharSequence(KEY_TITLE, getTitle());
            outState.putCharSequence(KEY_CONTENT, getContent());
        } catch (IllegalEditorStateException e) {
            AppLog.e(T.EDITOR, "onSaveInstanceState: unable to get title or content");
        }
    }

    private ActionBar getActionBar() {
        if (!isAdded()) {
            return null;
        }

        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (getView() != null) {
            // Reload the format bar to make sure the correct one for the new screen width is being used
            View formatBar = getView().findViewById(R.id.format_bar);

            if (formatBar != null) {
                // Remember the currently active format bar buttons so they can be re-activated after the reload
                ArrayList<String> activeTags = new ArrayList<>();
                for (Map.Entry<String, ToggleButton> entry : mTagToggleButtonMap.entrySet()) {
                    if (entry.getValue().checkedEh()) {
                        activeTags.add(entry.getKey());
                    }
                }

                ViewGroup parent = (ViewGroup) formatBar.getParent();
                parent.removeView(formatBar);

                formatBar = getActivity().getLayoutInflater().inflate(R.layout.format_bar, parent, false);
                formatBar.setId(R.id.format_bar);
                parent.addView(formatBar);

                setupFormatBarButtonMap(formatBar);

                if (mFormatBarDisabledEh) {
                    updateFormatBarEnabledState(false);
                }

                // Restore the active format bar buttons
                for (String tag : activeTags) {
                    mTagToggleButtonMap.get(tag).setChecked(true);
                }

                if (mSourceView.getVisibility() == View.VISIBLE) {
                    ToggleButton htmlButton = (ToggleButton) formatBar.findViewById(R.id.format_bar_button_html);
                    htmlButton.setChecked(true);
                }
            }

            // Reload HTML mode margins
            View sourceViewTitle = getView().findViewById(R.id.sourceview_title);
            View sourceViewContent = getView().findViewById(R.id.sourceview_content);

            if (sourceViewTitle != null && sourceViewContent != null) {
                int sideMargin = (int) getActivity().getResources().getDimension(R.dimen.sourceview_side_margin);

                ViewGroup.MarginLayoutParams titleParams =
                        (ViewGroup.MarginLayoutParams) sourceViewTitle.getLayoutParams();
                ViewGroup.MarginLayoutParams contentParams =
                        (ViewGroup.MarginLayoutParams) sourceViewContent.getLayoutParams();

                titleParams.setMargins(sideMargin, titleParams.topMargin, sideMargin, titleParams.bottomMargin);
                contentParams.setMargins(sideMargin, contentParams.topMargin, sideMargin, contentParams.bottomMargin);
            }
        }

        // Toggle action bar auto-hiding for the new orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        } else {
            mHideActionBarOnSoftKeyboardUp = false;
            showActionBarIfNeeded();
        }
    }

    private void setupFormatBarButtonMap(View view) {
        ToggleButton boldButton = (ToggleButton) view.findViewById(R.id.format_bar_button_bold);
        mTagToggleButtonMap.put(getString(R.string.format_bar_tag_bold), boldButton);

        ToggleButton italicButton = (ToggleButton) view.findViewById(R.id.format_bar_button_italic);
        mTagToggleButtonMap.put(getString(R.string.format_bar_tag_italic), italicButton);

        ToggleButton quoteButton = (ToggleButton) view.findViewById(R.id.format_bar_button_quote);
        mTagToggleButtonMap.put(getString(R.string.format_bar_tag_blockquote), quoteButton);

        ToggleButton ulButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ul);
        mTagToggleButtonMap.put(getString(R.string.format_bar_tag_unorderedList), ulButton);

        ToggleButton olButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ol);
        mTagToggleButtonMap.put(getString(R.string.format_bar_tag_orderedList), olButton);

        // Tablet-only
        ToggleButton strikethroughButton = (ToggleButton) view.findViewById(R.id.format_bar_button_strikethrough);
        if (strikethroughButton != null) {
            mTagToggleButtonMap.put(getString(R.string.format_bar_tag_strikethrough), strikethroughButton);
        }

        ToggleButton mediaButton = (ToggleButton) view.findViewById(R.id.format_bar_button_media);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_MEDIA, mediaButton);

        registerForContextMenu(mediaButton);

        ToggleButton linkButton = (ToggleButton) view.findViewById(R.id.format_bar_button_link);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_LINK, linkButton);

        ToggleButton htmlButton = (ToggleButton) view.findViewById(R.id.format_bar_button_html);
        htmlButton.setOnClickListener(this);

        for (ToggleButton button : mTagToggleButtonMap.values()) {
            button.setOnClickListener(this);
        }
    }

    protected void initJsEditor() {
        if (!isAdded()) {
            return;
        }

        ProfilingUtils.split("EditorFragment.initJsEditor");

        String htmlEditor = Utils.getHtmlFromFile(getActivity(), "android-editor.html");
        if (htmlEditor != null) {
            htmlEditor = htmlEditor.replace("%%TITLE%%", getString(R.string.visual_editor));
            htmlEditor = htmlEditor.replace("%%ANDROID_API_LEVEL%%", String.valueOf(Build.VERSION.SDK_INT));
            htmlEditor = htmlEditor.replace("%%LOCALIZED_STRING_INIT%%",
                    "nativeState.localizedStringEdit = '" + getString(R.string.edit) + "';\n" +
                    "nativeState.localizedStringUploading = '" + getString(R.string.uploading) + "';\n" +
                    "nativeState.localizedStringUploadingGallery = '" + getString(R.string.uploading_gallery_placeholder) + "';\n");
        }

        // To avoid reflection security issues with JavascriptInterface on API<17, we use an iframe to make URL requests
        // for callbacks from JS instead. These are received by WebViewClient.shouldOverrideUrlLoading() and then
        // passed on to the JsCallbackReceiver
        if (Build.VERSION.SDK_INT < 17) {
            mWebView.setJsCallbackReceiver(new JsCallbackReceiver(this));
        } else {
            mWebView.addJavascriptInterface(new JsCallbackReceiver(this), JS_CALLBACK_HANDLER);
        }

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");

        if (mDebugModeEnabled) {
            enableWebDebugging(true);
        }
    }

    public void checkForFailedUploadAndSwitchToHtmlMode(final ToggleButton toggleButton) {
        if (!isAdded()) {
            return;
        }

        // Show an Alert Dialog asking the user if he wants to remove all failed media before upload
        if (failedMediaUploadsEh()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.editor_failed_uploads_switch_html)
                    .setPositiveButton(R.string.editor_remove_failed_uploads, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Clear failed uploads and switch to HTML mode
                            removeAllFailedMediaUploads();
                            toggleHtmlMode(toggleButton);
                        }
                    }).setNegativeButton(android.R.string.cancel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    toggleButton.setChecked(false);
                }
            });
            builder.create().show();
        } else {
            toggleHtmlMode(toggleButton);
        }
    }

    public boolean actionInProgressEh() {
        return System.currentTimeMillis() - mActionStartedAt < MAX_ACTION_TIME_MS;
    }

    private void toggleHtmlMode(final ToggleButton toggleButton) {
        if (!isAdded()) {
            return;
        }

        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED);

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMedia.isEmpty() || actionInProgressEh()) {
            toggleButton.setChecked(false);
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        clearFormatBarButtons();
        updateFormatBarEnabledState(true);

        if (toggleButton.checkedEh()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded()) {
                        return;
                    }

                    // Update mTitle and mContentHtml with the latest state from the ZSSEditor
                    try {
                        getTitle();
                        getContent();
                    } catch (IllegalEditorStateException e) {
                        AppLog.e(T.EDITOR, "toggleHtmlMode: unable to get title or content");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleButton.setChecked(false);
                            }
                        });
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Set HTML mode state
                            mSourceViewTitle.setText(mTitle);

                            SpannableString spannableContent = new SpannableString(mContentHtml);
                            HtmlStyleUtils.styleHtmlForDisplay(spannableContent);
                            mSourceViewContent.setText(spannableContent);

                            mWebView.setVisibility(View.GONE);
                            mSourceView.setVisibility(View.VISIBLE);

                            mSourceViewContent.requestFocus();
                            mSourceViewContent.setSelection(0);

                            InputMethodManager imm = ((InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE));
                            imm.showSoftInput(mSourceViewContent, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }
            });

            thread.start();

        } else {
            mWebView.setVisibility(View.VISIBLE);
            mSourceView.setVisibility(View.GONE);

            mTitle = mSourceViewTitle.getText().toString();
            mContentHtml = mSourceViewContent.getText().toString();
            updateVisualEditorFields();

            // Update the list of failed media uploads
            mWebView.execJavaScriptFromString("ZSSEditor.getFailedMedia();");

            // Reset selection to avoid buggy cursor behavior
            mWebView.execJavaScriptFromString("ZSSEditor.resetSelectionOnField('zss_field_content');");
        }
    }

    private void displayLinkDialog() {
        final LinkDialogFragment linkDialogFragment = new LinkDialogFragment();
        linkDialogFragment.setTargetFragment(this, LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_ADD);

        final Bundle dialogBundle = new Bundle();

        // Pass potential URL from user clipboard
        String clipboardUri = Utils.getUrlFromClipboard(getActivity());
        if (clipboardUri != null) {
            dialogBundle.putString(LinkDialogFragment.LINK_DIALOG_ARG_URL, clipboardUri);
        }

        // Pass selected text to dialog
        if (mSourceView.getVisibility() == View.VISIBLE) {
            // HTML mode
            mSelectionStart = mSourceViewContent.getSelectionStart();
            mSelectionEnd = mSourceViewContent.getSelectionEnd();

            String selectedText = mSourceViewContent.getText().toString().substring(mSelectionStart, mSelectionEnd);
            dialogBundle.putString(LinkDialogFragment.LINK_DIALOG_ARG_TEXT, selectedText);

            linkDialogFragment.setArguments(dialogBundle);
            linkDialogFragment.show(getFragmentManager(), LinkDialogFragment.class.getSimpleName());
        } else {
            // Visual mode
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded()) {
                        return;
                    }

                    mGetSelectedTextCountDownLatch = new CountDownLatch(1);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.execJavaScriptFromString(
                                    "ZSSEditor.execFunctionForResult('getSelectedTextToLinkify');");
                        }
                    });

                    try {
                        if (mGetSelectedTextCountDownLatch.await(1, TimeUnit.SECONDS)) {
                            dialogBundle.putString(LinkDialogFragment.LINK_DIALOG_ARG_TEXT, mJavaScriptResult);
                        }
                    } catch (InterruptedException e) {
                        AppLog.d(T.EDITOR, "Failed to obtain selected text from JS editor.");
                    }

                    linkDialogFragment.setArguments(dialogBundle);
                    linkDialogFragment.show(getFragmentManager(), LinkDialogFragment.class.getSimpleName());
                }
            });

            thread.start();
        }
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) {
            return;
        }

        int id = v.getId();
        if (id == R.id.format_bar_button_html) {
            checkForFailedUploadAndSwitchToHtmlMode((ToggleButton) v);
        } else if (id == R.id.format_bar_button_media) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
            ((ToggleButton) v).setChecked(false);

            if (actionInProgressEh()) {
                ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
                return;
            }

            if (mSourceView.getVisibility() == View.VISIBLE) {
                ToastUtils.showToast(getActivity(), R.string.alert_insert_image_html_mode, ToastUtils.Duration.LONG);
            } else {
                mEditorFragmentListener.onAddMediaClicked();
                getActivity().openContextMenu(mTagToggleButtonMap.get(TAG_FORMAT_BAR_BUTTON_MEDIA));
            }
        } else if (id == R.id.format_bar_button_link) {
            if (!((ToggleButton) v).checkedEh()) {
                // The link button was checked when it was pressed; remove the current link
                mWebView.execJavaScriptFromString("ZSSEditor.unlink();");
                mEditorFragmentListener.onTrackableEvent(TrackableEvent.UNLINK_BUTTON_TAPPED);
                return;
            }
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.LINK_BUTTON_TAPPED);

            ((ToggleButton) v).setChecked(false);

            displayLinkDialog();
        } else {
            if (v instanceof ToggleButton) {
                onFormattingButtonClicked((ToggleButton) v);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // If the WebView or EditText has received a touch event, the keyboard will be displayed and the action bar
            // should hide
            mKeyboardOpenEh = true;
            hideActionBarIfNeeded();
        }
        return false;
    }

    /**
     * Intercept back button press while soft keyboard is visible.
     */
    @Override
    public void onImeBack() {
        mKeyboardOpenEh = false;
        showActionBarIfNeeded();
    }

    @Override
    public String onAuthHeaderRequested(String url) {
        return mEditorFragmentListener.onAuthHeaderRequested(url);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_ADD ||
                requestCode == LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_UPDATE)) {

            if (resultCode == LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_DELETE) {
                mWebView.execJavaScriptFromString("ZSSEditor.unlink();");
                return;
            }

            if (data == null) {
                return;
            }

            Bundle extras = data.getExtras();
            if (extras == null) {
                return;
            }

            String linkUrl = extras.getString(LinkDialogFragment.LINK_DIALOG_ARG_URL);
            String linkText = extras.getString(LinkDialogFragment.LINK_DIALOG_ARG_TEXT);

            if (linkText == null || linkText.equals("")) {
                linkText = linkUrl;
            }

            if (TextUtils.isEmpty(Uri.parse(linkUrl).getScheme())) linkUrl = "http://" + linkUrl;

            if (mSourceView.getVisibility() == View.VISIBLE) {
                Editable content = mSourceViewContent.getText();
                if (content == null) {
                    return;
                }

                if (mSelectionStart < mSelectionEnd) {
                    content.delete(mSelectionStart, mSelectionEnd);
                }

                String urlHtml = "<a href=\"" + linkUrl + "\">" + linkText + "</a>";

                content.insert(mSelectionStart, urlHtml);
                mSourceViewContent.setSelection(mSelectionStart + urlHtml.length());
            } else {
                String jsMethod;
                if (requestCode == LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_ADD) {
                    jsMethod = "ZSSEditor.insertLink";
                } else {
                    jsMethod = "ZSSEditor.updateLink";
                }
                mWebView.execJavaScriptFromString(jsMethod + "('" + Utils.escapeHtml(linkUrl) + "', '" +
                        Utils.escapeHtml(linkText) + "');");
            }
        } else if (requestCode == ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE) {
            if (data == null) {
                mWebView.execJavaScriptFromString("ZSSEditor.clearCurrentEditingImage();");
                return;
            }

            Bundle extras = data.getExtras();
            if (extras == null) {
                return;
            }

            final String imageMeta = Utils.escapeQuotes(StringUtils.notNullStr(extras.getString("imageMeta")));
            final int imageRemoteId = extras.getInt("imageRemoteId");
            final boolean featuredImageEh = extras.getBoolean("featuredEh");

            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.execJavaScriptFromString("ZSSEditor.updateCurrentImageMeta('" + imageMeta + "');");
                }
            });

            if (imageRemoteId != 0) {
                if (featuredImageEh) {
                    mFeaturedImageId = imageRemoteId;
                    mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                } else {
                    // If this image was unset as featured, clear the featured image id
                    if (mFeaturedImageId == imageRemoteId) {
                        mFeaturedImageId = 0;
                        mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void enableWebDebugging(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppLog.i(T.EDITOR, "Enabling web debugging");
            WebView.setWebContentsDebuggingEnabled(enable);
        }
        mWebView.setDebugModeEnabled(mDebugModeEnabled);
    }

    @Override
    public void setTitle(CharSequence text) {
        mTitle = text.toString();
    }

    @Override
    public void setContent(CharSequence text) {
        mContentHtml = text.toString();
    }

    /**
     * Returns the contents of the title field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getTitle() throws IllegalEditorStateException {
        if (!isAdded()) {
            throw new IllegalEditorStateException();
        }

        if (mSourceView != null && mSourceView.getVisibility() == View.VISIBLE) {
            mTitle = mSourceViewTitle.getText().toString();
            return StringUtils.notNullStr(mTitle);
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            AppLog.d(T.EDITOR, "getTitle() called from UI thread");
        }

        mGetTitleCountDownLatch = new CountDownLatch(1);

        // All WebView methods must be called from the UI thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_title').getHTMLForCallback();");
            }
        });

        try {
            mGetTitleCountDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.EDITOR, e);
            Thread.currentThread().interrupt();
        }

        return StringUtils.notNullStr(mTitle.replaceAll("&nbsp;$", ""));
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent() throws IllegalEditorStateException {
        if (!isAdded()) {
            throw new IllegalEditorStateException();
        }

        if (mSourceView != null && mSourceView.getVisibility() == View.VISIBLE) {
            mContentHtml = mSourceViewContent.getText().toString();
            return StringUtils.notNullStr(mContentHtml);
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            AppLog.d(T.EDITOR, "getContent() called from UI thread");
        }

        mGetContentCountDownLatch = new CountDownLatch(1);

        // All WebView methods must be called from the UI thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').getHTMLForCallback();");
            }
        });

        try {
            mGetContentCountDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.EDITOR, e);
            Thread.currentThread().interrupt();
        }

        return StringUtils.notNullStr(mContentHtml);
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        if (!mDomHasLoaded) {
            // If the DOM hasn't loaded yet, we won't be able to add media to the ZSSEditor
            // Place them in a queue to be handled when the DOM loaded callback is received
            mWaitingMediaFiles.put(mediaUrl, mediaFile);
            return;
        }

        final String safeMediaUrl = Utils.escapeQuotes(mediaUrl);

        mWebView.post(new Runnable() {
            @Override
            public void run() {
                String mediaId = String.valueOf(mediaFile.getId());
                if (URLUtil.isNetworkUrl(mediaUrl)) {
                    if (mediaFile.videoEh()) {
                        String posterUrl = Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL()));
                        String videoPressId = ShortcodeUtils.getVideoPressIdFromShortCode(
                                mediaFile.getVideoPressShortCode());

                        mWebView.execJavaScriptFromString("ZSSEditor.insertVideo('" + safeMediaUrl + "', '" +
                                posterUrl + "', '" + videoPressId +  "');");
                    } else {
                        mWebView.execJavaScriptFromString("ZSSEditor.insertImage('" + safeMediaUrl + "', '" + mediaId +
                                "');");
                    }
                    mActionStartedAt = System.currentTimeMillis();
                } else {
                    if (mediaFile.videoEh()) {
                        String posterUrl = Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL()));
                        mWebView.execJavaScriptFromString("ZSSEditor.insertLocalVideo(" + mediaId + ", '" + posterUrl +
                                "');");
                        mUploadingMedia.put(mediaId, MediaType.VIDEO);
                    } else {
                        mWebView.execJavaScriptFromString("ZSSEditor.insertLocalImage(" + mediaId + ", '" +
                                safeMediaUrl + "');");
                        mUploadingMedia.put(mediaId, MediaType.IMAGE);
                    }
                }
            }
        });
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        if (!mDomHasLoaded) {
            // If the DOM hasn't loaded yet, we won't be able to add a gallery to the ZSSEditor
            // Place it in a queue to be handled when the DOM loaded callback is received
            mWaitingGalleries.add(mediaGallery);
            return;
        }

        if (mediaGallery.getIds().isEmpty()) {
            mUploadingMediaGallery = mediaGallery;
            mWebView.execJavaScriptFromString("ZSSEditor.insertLocalGallery('" + mediaGallery.getUniqueId() + "');");
        } else {
            // Ensure that the content field is in focus (it may not be if we're adding a gallery to a new post by a
            // share action and not via the format bar button)
            mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').focus();");

            mWebView.execJavaScriptFromString("ZSSEditor.insertGallery('" + mediaGallery.getIdsStr() + "', '" +
                    mediaGallery.getType() + "', " + mediaGallery.getNumColumns() + ");");
        }
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                    mWebView.execJavaScriptFromString("ZSSEditor.setVideoPressLinks('" + videoId + "', '" +
                        Utils.escapeQuotes(videoUrl) + "', '" + Utils.escapeQuotes(posterUrl) + "');");
            }
        });
    }

    @Override
    public boolean uploadingMediaEh() {
        return (mUploadingMedia.size() > 0);
    }

    @Override
    public boolean failedMediaUploadsEh() {
        return (mFailedMediaIds.size() > 0);
    }

    @Override
    public void removeAllFailedMediaUploads() {
        mWebView.execJavaScriptFromString("ZSSEditor.removeAllFailedMediaUploads();");
    }

    @Override
    public Spanned getSpannedContent() {
        return null;
    }

    @Override
    public void setTitlePlaceholder(CharSequence placeholderText) {
        mTitlePlaceholder = placeholderText.toString();
    }

    @Override
    public void setContentPlaceholder(CharSequence placeholderText) {
        mContentPlaceholder = placeholderText.toString();
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        if(!isAdded()) {
            return;
        }

        final String mimeType = mediaFile.getMimeType();
        if (TextUtils.isEmpty(mimeType)) {
            return;
        }

        mWebView.post(new Runnable() {
            @Override
            public void run() {
                String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
                if (mimeType.contains("image")) {
                    String remoteMediaId = mediaFile.getMediaId();
                    mWebView.execJavaScriptFromString("ZSSEditor.replaceLocalImageWithRemoteImage(" + localMediaId +
                            ", '" + remoteMediaId + "', '" + remoteUrl + "');");
                } else if (mimeType.contains("video")) {
                    String posterUrl = Utils.escapeQuotes(StringUtils.notNullStr(mediaFile.getThumbnailURL()));
                    String videoPressId = ShortcodeUtils.getVideoPressIdFromShortCode(
                            mediaFile.getVideoPressShortCode());
                    mWebView.execJavaScriptFromString("ZSSEditor.replaceLocalVideoWithRemoteVideo(" + localMediaId +
                            ", '" + remoteUrl + "', '" + posterUrl + "', '" + videoPressId + "');");
                }
            }
        });
    }

    @Override
    public void onMediaUploadProgress(final String mediaId, final float progress) {
        if(!isAdded()) {
            return;
        }

        final MediaType mediaType = mUploadingMedia.get(mediaId);
        if (mediaType != null) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    String progressString = String.format(Locale.US, "%.1f", progress);
                    mWebView.execJavaScriptFromString("ZSSEditor.setProgressOnMedia(" + mediaId + ", " +
                            progressString + ");");
                }
            });
        }
    }

    @Override
    public void onMediaUploadFailed(final String mediaId, final String errorMessage) {
        if(!isAdded()) {
            return;
        }
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                MediaType mediaType = mUploadingMedia.get(mediaId);
                if (mediaType != null) {
                    switch (mediaType) {
                        case IMAGE:
                            mWebView.execJavaScriptFromString("ZSSEditor.markImageUploadFailed(" + mediaId + ", '"
                                    + Utils.escapeQuotes(errorMessage) + "');");
                            break;
                        case VIDEO:
                            mWebView.execJavaScriptFromString("ZSSEditor.markVideoUploadFailed(" + mediaId + ", '"
                                    + Utils.escapeQuotes(errorMessage) + "');");
                    }
                    mFailedMediaIds.add(mediaId);
                    mUploadingMedia.remove(mediaId);
                }
            }
        });
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
        if(!isAdded()) {
            return;
        }

        if (galleryId == mUploadingMediaGallery.getUniqueId()) {
            // TODO: media IDs are Long's; should we update to use ArrayList<Long>?
            ArrayList<String> mediaIds = mUploadingMediaGallery.getIds();
            mediaIds.add(String.valueOf(remoteMediaId));
            mUploadingMediaGallery.setIds(mediaIds);

            if (remaining == 0) {
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.execJavaScriptFromString("ZSSEditor.replacePlaceholderGallery('" + galleryId + "', '" +
                                mUploadingMediaGallery.getIdsStr() + "', '" +
                                mUploadingMediaGallery.getType() + "', " +
                                mUploadingMediaGallery.getNumColumns() + ");");
                    }
                });
            }
        }
    }

    public void onDomLoaded() {
        ProfilingUtils.split("EditorFragment.onDomLoaded");

        mWebView.post(new Runnable() {
            public void run() {
                if (!isAdded()) {
                    return;
                }

                mDomHasLoaded = true;

                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setMultiline('true');");

                // Set title and content placeholder text
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_title').setPlaceholderText('" +
                        Utils.escapeQuotes(mTitlePlaceholder) + "');");
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setPlaceholderText('" +
                        Utils.escapeQuotes(mContentPlaceholder) + "');");

                // Load title and content into ZSSEditor
                updateVisualEditorFields();

                // If there are images that are still in progress (because the editor exited before they completed),
                // set them to failed, so the user can restart them (otherwise they will stay stuck in 'uploading' mode)
                mWebView.execJavaScriptFromString("ZSSEditor.markAllUploadingMediaAsFailed('"
                        + Utils.escapeQuotes(getString(R.string.tap_to_try_again)) + "');");

                // Update the list of failed media uploads
                mWebView.execJavaScriptFromString("ZSSEditor.getFailedMedia();");

                hideActionBarIfNeeded();

                // Reset all format bar buttons (in case they remained active through activity re-creation)
                ToggleButton htmlButton = (ToggleButton) getActivity().findViewById(R.id.format_bar_button_html);
                htmlButton.setChecked(false);
                for (ToggleButton button : mTagToggleButtonMap.values()) {
                    button.setChecked(false);
                }

                boolean editorHasFocus = false;

                // Add any media files that were placed in a queue due to the DOM not having loaded yet
                if (mWaitingMediaFiles.size() > 0) {
                    // Image insertion will only work if the content field is in focus
                    // (for a new post, no field is in focus until user action)
                    mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').focus();");
                    editorHasFocus = true;

                    for (Map.Entry<String, MediaFile> entry : mWaitingMediaFiles.entrySet()) {
                        appendMediaFile(entry.getValue(), entry.getKey(), null);
                    }
                    mWaitingMediaFiles.clear();
                }

                // Add any galleries that were placed in a queue due to the DOM not having loaded yet
                if (mWaitingGalleries.size() > 0) {
                    // Gallery insertion will only work if the content field is in focus
                    // (for a new post, no field is in focus until user action)
                    mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').focus();");
                    editorHasFocus = true;

                    for (MediaGallery mediaGallery : mWaitingGalleries) {
                        appendGallery(mediaGallery);
                    }

                    mWaitingGalleries.clear();
                }

                if (!editorHasFocus) {
                    mWebView.execJavaScriptFromString("ZSSEditor.focusFirstEditableField();");
                }

                // Show the keyboard
                ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(mWebView, InputMethodManager.SHOW_IMPLICIT);

                ProfilingUtils.split("EditorFragment.onDomLoaded completed");
                ProfilingUtils.dump();
                ProfilingUtils.stop();
            }
        });
    }

    public void onSelectionStyleChanged(final Map<String, Boolean> changeMap) {
        mWebView.post(new Runnable() {
            public void run() {
                for (Map.Entry<String, Boolean> entry : changeMap.entrySet()) {
                    // Handle toggling format bar style buttons
                    ToggleButton button = mTagToggleButtonMap.get(entry.getKey());
                    if (button != null) {
                        button.setChecked(entry.getValue());
                    }
                }
            }
        });
    }

    public void onSelectionChanged(final Map<String, String> selectionArgs) {
        mFocusedFieldId = selectionArgs.get("id"); // The field now in focus
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (!mFocusedFieldId.isEmpty()) {
                    switch (mFocusedFieldId) {
                        case "zss_field_title":
                            updateFormatBarEnabledState(false);
                            break;
                        case "zss_field_content":
                            updateFormatBarEnabledState(true);
                            break;
                    }
                }
            }
        });
    }

    public void onMediaTapped(final String mediaId, final MediaType mediaType, final JSONObject meta, String uploadStatus) {
        if (mediaType == null || !isAdded()) {
            return;
        }

        switch (uploadStatus) {
            case "uploading":
                // Display 'cancel upload' dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.stop_upload_dialog_title));
                builder.setPositiveButton(R.string.stop_upload_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mEditorFragmentListener.onMediaUploadCancelClicked(mediaId, true);

                        mWebView.post(new Runnable() {
                            @Override
                            public void run() {
                                switch (mediaType) {
                                    case IMAGE:
                                        mWebView.execJavaScriptFromString("ZSSEditor.removeImage(" + mediaId + ");");
                                        break;
                                    case VIDEO:
                                        mWebView.execJavaScriptFromString("ZSSEditor.removeVideo(" + mediaId + ");");
                                }
                                mUploadingMedia.remove(mediaId);
                            }
                        });
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "failed":
                // Retry media upload
                mEditorFragmentListener.onMediaRetryClicked(mediaId);

                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        switch (mediaType) {
                            case IMAGE:
                                mWebView.execJavaScriptFromString("ZSSEditor.unmarkImageUploadFailed(" + mediaId
                                        + ");");
                                break;
                            case VIDEO:
                                mWebView.execJavaScriptFromString("ZSSEditor.unmarkVideoUploadFailed(" + mediaId
                                        + ");");
                        }
                        mFailedMediaIds.remove(mediaId);
                        mUploadingMedia.put(mediaId, mediaType);
                    }
                });
                break;
            default:
                if (!mediaType.equals(MediaType.IMAGE)) {
                    return;
                }

                // Only show image options fragment for image taps
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG) != null) {
                    return;
                }
                mEditorFragmentListener.onTrackableEvent(TrackableEvent.IMAGE_EDITED);
                ImageSettingsDialogFragment imageSettingsDialogFragment = new ImageSettingsDialogFragment();
                imageSettingsDialogFragment.setTargetFragment(this,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE);

                Bundle dialogBundle = new Bundle();

                dialogBundle.putString("maxWidth", mBlogSettingMaxImageWidth);
                dialogBundle.putBoolean("featuredImageSupported", mFeaturedImageSupported);

                // Request and add an authorization header for HTTPS images
                // Use https:// when requesting the auth header, in case the image is incorrectly using http://.
                // If an auth header is returned, force https:// for the actual HTTP request.
                HashMap<String, String> headerMap = new HashMap<>();
                if (mCustomHttpHeaders != null) {
                    headerMap.putAll(mCustomHttpHeaders);
                }

                try {
                    final String imageSrc = meta.getString("src");
                    String authHeader = mEditorFragmentListener.onAuthHeaderRequested(UrlUtils.makeHttps(imageSrc));
                    if (authHeader.length() > 0) {
                        meta.put("src", UrlUtils.makeHttps(imageSrc));
                        headerMap.put("Authorization", authHeader);
                    }
                } catch (JSONException e) {
                    AppLog.e(T.EDITOR, "Could not retrieve image url from JSON metadata");
                }
                dialogBundle.putSerializable("headerMap", headerMap);

                dialogBundle.putString("imageMeta", meta.toString());

                String imageId = JSONUtils.getString(meta, "attachment_id");
                if (!imageId.isEmpty()) {
                    dialogBundle.putBoolean("featuredEh", mFeaturedImageId == Integer.parseInt(imageId));
                }

                imageSettingsDialogFragment.setArguments(dialogBundle);

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                fragmentTransaction.add(android.R.id.content, imageSettingsDialogFragment,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG)
                        .addToBackStack(null)
                        .commit();

                mWebView.notifyVisibilityChanged(false);
                break;
        }
    }

    public void onLinkTapped(String url, String title) {
        LinkDialogFragment linkDialogFragment = new LinkDialogFragment();
        linkDialogFragment.setTargetFragment(this, LinkDialogFragment.LINK_DIALOG_REQUEST_CODE_UPDATE);

        Bundle dialogBundle = new Bundle();

        dialogBundle.putString(LinkDialogFragment.LINK_DIALOG_ARG_URL, url);
        dialogBundle.putString(LinkDialogFragment.LINK_DIALOG_ARG_TEXT, title);

        linkDialogFragment.setArguments(dialogBundle);
        linkDialogFragment.show(getFragmentManager(), "LinkDialogFragment");
    }

    @Override
    public void onMediaRemoved(String mediaId) {
        mUploadingMedia.remove(mediaId);
        mFailedMediaIds.remove(mediaId);
        mEditorFragmentListener.onMediaUploadCancelClicked(mediaId, true);
    }

    @Override
    public void onMediaReplaced(String mediaId) {
        mUploadingMedia.remove(mediaId);
    }

    @Override
    public void onVideoPressInfoRequested(final String videoId) {
        mEditorFragmentListener.onVideoPressInfoRequested(videoId);
    }

    public void onGetHtmlResponse(Map<String, String> inputArgs) {
        String functionId = inputArgs.get("function");

        if (functionId.isEmpty()) {
            return;
        }

        switch (functionId) {
            case "getHTMLForCallback":
                String fieldId = inputArgs.get("id");
                String fieldContents = inputArgs.get("contents");
                if (!fieldId.isEmpty()) {
                    switch (fieldId) {
                        case "zss_field_title":
                            mTitle = fieldContents;
                            mGetTitleCountDownLatch.countDown();
                            break;
                        case "zss_field_content":
                            mContentHtml = fieldContents;
                            mGetContentCountDownLatch.countDown();
                            break;
                    }
                }
                break;
            case "getSelectedTextToLinkify":
                mJavaScriptResult = inputArgs.get("result");
                mGetSelectedTextCountDownLatch.countDown();
                break;
            case "getFailedMedia":
                String[] mediaIds = inputArgs.get("ids").split(",");
                for (String mediaId : mediaIds) {
                    if (!mediaId.equals("")) {
                        mFailedMediaIds.add(mediaId);
                    }
                }
        }
    }

    public void setWebViewErrorListener(ErrorListener errorListener) {
        mWebView.setErrorListener(errorListener);
    }

    private void updateVisualEditorFields() {
        mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_title').setPlainText('" +
                Utils.escapeHtml(mTitle) + "');");
        mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setHTML('" +
                Utils.escapeHtml(mContentHtml) + "');");
    }

    /**
     * Hide the action bar if needed.
     */
    private void hideActionBarIfNeeded() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null
                && !hardwareKeyboardPresentEh()
                && mHideActionBarOnSoftKeyboardUp
                && mIsKeyboardOpen
                && actionBar.isShowing()) {
            getActionBar().hide();
        }
    }

    /**
     * Show the action bar if needed.
     */
    private void showActionBarIfNeeded() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null && !actionBar.isShowing()) {
            actionBar.show();
        }
    }

    /**
     * Returns true if a hardware keyboard is detected, otherwise false.
     */
    private boolean hardwareKeyboardPresentEh() {
        Configuration config = getResources().getConfiguration();
        boolean returnValue = false;
        if (config.keyboard != Configuration.KEYBOARD_NOKEYS) {
            returnValue = true;
        }
        return returnValue;
    }

    void updateFormatBarEnabledState(boolean enabled) {
        float alpha = (enabled ? TOOLBAR_ALPHA_ENABLED : TOOLBAR_ALPHA_DISABLED);
        for(ToggleButton button : mTagToggleButtonMap.values()) {
            button.setEnabled(enabled);
            button.setAlpha(alpha);
        }

        mFormatBarDisabledEh = !enabled;
    }

    private void clearFormatBarButtons() {
        for (ToggleButton button : mTagToggleButtonMap.values()) {
            if (button != null) {
                button.setChecked(false);
            }
        }
    }

    private void onFormattingButtonClicked(ToggleButton toggleButton) {
        String tag = toggleButton.getTag().toString();
        buttonTappedListener(toggleButton);
        if (mWebView.getVisibility() == View.VISIBLE) {
            mWebView.execJavaScriptFromString("ZSSEditor.set" + StringUtils.capitalize(tag) + "();");
        } else {
            applyFormattingHtmlMode(toggleButton, tag);
        }
    }

    private void buttonTappedListener(ToggleButton toggleButton) {
        int id = toggleButton.getId();
        if (id == R.id.format_bar_button_bold) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.BOLD_BUTTON_TAPPED);
        } else if (id == R.id.format_bar_button_italic) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.ITALIC_BUTTON_TAPPED);
        } else if (id == R.id.format_bar_button_ol) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.OL_BUTTON_TAPPED);
        } else if (id == R.id.format_bar_button_ul) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.UL_BUTTON_TAPPED);
        } else if (id == R.id.format_bar_button_quote) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.BLOCKQUOTE_BUTTON_TAPPED);
        } else if (id == R.id.format_bar_button_strikethrough) {
            mEditorFragmentListener.onTrackableEvent(TrackableEvent.STRIKETHROUGH_BUTTON_TAPPED);
        }
    }

    /**
     * In HTML mode, applies formatting to selected text, or inserts formatting tag at current cursor position
     * @param toggleButton format bar button which was clicked
     * @param tag identifier tag
     */
    private void applyFormattingHtmlMode(ToggleButton toggleButton, String tag) {
        if (mSourceViewContent == null) {
            return;
        }

        // Replace style tags with their proper HTML tags
        String htmlTag;
        if (tag.equals(getString(R.string.format_bar_tag_bold))) {
            htmlTag = "b";
        } else if (tag.equals(getString(R.string.format_bar_tag_italic))) {
            htmlTag = "i";
        } else if (tag.equals(getString(R.string.format_bar_tag_strikethrough))) {
            htmlTag = "del";
        } else if (tag.equals(getString(R.string.format_bar_tag_unorderedList))) {
            htmlTag = "ul";
        } else if (tag.equals(getString(R.string.format_bar_tag_orderedList))) {
            htmlTag = "ol";
        } else {
            htmlTag = tag;
        }

        int selectionStart = mSourceViewContent.getSelectionStart();
        int selectionEnd = mSourceViewContent.getSelectionEnd();

        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        boolean textIsSelected = selectionEnd > selectionStart;

        String startTag = "<" + htmlTag + ">";
        String endTag = "</" + htmlTag + ">";

        // Add li tags together with ul and ol tags
        if (htmlTag.equals("ul") || htmlTag.equals("ol")) {
            startTag = startTag + "\n\t<li>";
            endTag = "</li>\n" + endTag;
        }

        Editable content = mSourceViewContent.getText();
        if (textIsSelected) {
            // Surround selected text with opening and closing tags
            content.insert(selectionStart, startTag);
            content.insert(selectionEnd + startTag.length(), endTag);
            toggleButton.setChecked(false);
            mSourceViewContent.setSelection(selectionEnd + startTag.length() + endTag.length());
        } else if (toggleButton.checkedEh()) {
            // Insert opening tag
            content.insert(selectionStart, startTag);
            mSourceViewContent.setSelection(selectionEnd + startTag.length());
        } else {
            // Insert closing tag
            content.insert(selectionEnd, endTag);
            mSourceViewContent.setSelection(selectionEnd + endTag.length());
        }
    }

    @Override
    public void onActionFinished() {
        mActionStartedAt = -1;
    }
}
