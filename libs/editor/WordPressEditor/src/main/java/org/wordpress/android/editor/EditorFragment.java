package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EditorFragment extends EditorFragmentAbstract implements View.OnClickListener, View.OnTouchListener,
        OnJsEditorStateChangedListener, OnImeBackListener {
    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private static final String TAG_FORMAT_BAR_BUTTON_MEDIA = "media";
    private static final String TAG_FORMAT_BAR_BUTTON_LINK = "link";

    private static final float TOOLBAR_ALPHA_ENABLED = 1;
    private static final float TOOLBAR_ALPHA_DISABLED = 0.5f;

    private String mTitle = "";
    private String mContentHtml = "";

    private EditorWebViewAbstract mWebView;
    private View mSourceView;
    private SourceViewEditText mSourceViewTitle;
    private SourceViewEditText mSourceViewContent;

    private String mTitlePlaceholder = "";
    private String mContentPlaceholder = "";

    private boolean mHideActionBarOnSoftKeyboardUp;

    private CountDownLatch mGetTitleCountDownLatch;
    private CountDownLatch mGetContentCountDownLatch;

    private final Map<String, ToggleButton> mTagToggleButtonMap = new HashMap<>();

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

        mWebView.setOnTouchListener(this);
        mWebView.setOnImeBackListener(this);

        // Ensure that the content field is always filling the remaining screen space
        mWebView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.execJavaScriptFromString("ZSSEditor.refreshVisibleViewportSize();");
                    }
                });
            }
        });

        mEditorFragmentListener.onEditorFragmentInitialized();

        initJsEditor();

        // -- HTML mode configuration

        mSourceView = view.findViewById(R.id.sourceview);
        mSourceViewTitle = (SourceViewEditText) view.findViewById(R.id.sourceview_title);
        mSourceViewContent = (SourceViewEditText) view.findViewById(R.id.sourceview_content);

        // Toggle format bar on/off as user changes focus between title and content in HTML mode
        mSourceViewTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                updateFormatBarEnabledState(!hasFocus);
            }
        });

        mSourceViewTitle.setOnTouchListener(this);
        mSourceViewContent.setOnTouchListener(this);

        mSourceViewTitle.setOnImeBackListener(this);
        mSourceViewContent.setOnImeBackListener(this);

        mSourceViewContent.addTextChangedListener(new HtmlStyleTextWatcher());

        mSourceViewTitle.setHint(mTitlePlaceholder);

        // -- Format bar configuration

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

        ToggleButton linkButton = (ToggleButton) view.findViewById(R.id.format_bar_button_link);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_LINK, linkButton);

        ToggleButton htmlButton = (ToggleButton) view.findViewById(R.id.format_bar_button_html);
        htmlButton.setOnClickListener(this);

        for (ToggleButton button : mTagToggleButtonMap.values()) {
            button.setOnClickListener(this);
        }

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
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

    protected void initJsEditor() {
        if(!isAdded()) {
            return;
        }

        String htmlEditor = Utils.getHtmlFromFile(getActivity(), "android-editor.html");

        mWebView.addJavascriptInterface(new JsCallbackReceiver(this), JS_CALLBACK_HANDLER);

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");

        enableWebDebugging(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.format_bar_button_html) {
            clearFormatBarButtons();
            updateFormatBarEnabledState(true);

            if (((ToggleButton) v).isChecked()) {
                mSourceViewTitle.setText(getTitle());

                SpannableString spannableContent = new SpannableString(getContent());
                HtmlStyleUtils.styleHtmlForDisplay(spannableContent);
                mSourceViewContent.setText(spannableContent);

                mWebView.setVisibility(View.GONE);
                mSourceView.setVisibility(View.VISIBLE);

                mSourceViewContent.requestFocus();
                mSourceViewContent.setSelection(0);

                InputMethodManager imm = ((InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE));
                imm.showSoftInput(mSourceViewContent, InputMethodManager.SHOW_IMPLICIT);
            } else {
                mWebView.setVisibility(View.VISIBLE);
                mSourceView.setVisibility(View.GONE);

                mTitle = mSourceViewTitle.getText().toString();
                mContentHtml = mSourceViewContent.getText().toString();
                updateVisualEditorFields();

                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').focus();");
            }
        } else if (id == R.id.format_bar_button_media) {
            // TODO: Handle inserting media
            ((ToggleButton) v).setChecked(false);
        } else if (id == R.id.format_bar_button_link) {
            // TODO: Handle inserting a link
            ((ToggleButton) v).setChecked(false);
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
            hideActionBarIfNeeded();
        }
        return false;
    }

    /**
     * Intercept back button press while soft keyboard is visible.
     */
    @Override
    public void onImeBack() {
        showActionBarIfNeeded();
    }

    @SuppressLint("NewApi")
    private void enableWebDebugging(boolean enable) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppLog.i(T.EDITOR, "Enabling web debugging");
            WebView.setWebContentsDebuggingEnabled(enable);
        }
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
    public CharSequence getTitle() {
        if (!isAdded()) {
            return "";
        }

        if (mSourceView.getVisibility() == View.VISIBLE) {
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

        return StringUtils.notNullStr(mTitle);
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent() {
        if (!isAdded()) {
            return "";
        }

        if (mSourceView.getVisibility() == View.VISIBLE) {
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
    public void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader) {
        // TODO
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        // TODO
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

    public void onDomLoaded() {
        mWebView.post(new Runnable() {
            public void run() {
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setMultiline('true');");

                // Set title and content placeholder text
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_title').setPlaceholderText('" +
                        mTitlePlaceholder + "');");
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setPlaceholderText('" +
                        mContentPlaceholder + "');");

                // Load title and content into ZSSEditor
                updateVisualEditorFields();
                hideActionBarIfNeeded();

                // Reset all format bar buttons (in case they remained active through activity re-creation)
                ToggleButton htmlButton = (ToggleButton) getActivity().findViewById(R.id.format_bar_button_html);
                htmlButton.setChecked(false);
                for (ToggleButton button : mTagToggleButtonMap.values()) {
                    button.setChecked(false);
                }
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
        final String focusedFieldId = selectionArgs.get("id"); // The field now in focus
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (!focusedFieldId.isEmpty()) {
                    switch(focusedFieldId) {
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

    public void onGetHtmlResponse(Map<String, String> inputArgs) {
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
        if (getActionBar() != null
                && !isHardwareKeyboardPresent()
                && mHideActionBarOnSoftKeyboardUp
                && actionBar.isShowing()) {
            getActionBar().hide();
        }
    }

    /**
     * Show the action bar if needed.
     */
    private void showActionBarIfNeeded() {

        ActionBar actionBar = getActionBar();
        if (getActionBar() != null && !actionBar.isShowing()) {
            getActionBar().show();
        }
    }

    /**
     * Returns true if a hardware keyboard is detected, otherwise false.
     */
    private boolean isHardwareKeyboardPresent() {
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

        if (mWebView.getVisibility() == View.VISIBLE) {
            mWebView.execJavaScriptFromString("ZSSEditor.set" + StringUtils.capitalize(tag) + "();");
        } else {
            applyFormattingHtmlMode(toggleButton, tag);
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
        } else if (toggleButton.isChecked()) {
            // Insert opening tag
            content.insert(selectionStart, startTag);
            mSourceViewContent.setSelection(selectionEnd + startTag.length());
        } else {
            // Insert closing tag
            content.insert(selectionEnd, endTag);
            mSourceViewContent.setSelection(selectionEnd + endTag.length());
        }
    }
}
