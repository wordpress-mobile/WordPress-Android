package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.HashMap;
import java.util.Map;

public class EditorFragment extends EditorFragmentAbstract implements View.OnClickListener,
        OnJsEditorStateChangedListener {
    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private static final String TAG_FORMAT_BAR_BUTTON_MEDIA = "media";
    private static final String TAG_FORMAT_BAR_BUTTON_BOLD = "bold";
    private static final String TAG_FORMAT_BAR_BUTTON_ITALIC = "italic";
    private static final String TAG_FORMAT_BAR_BUTTON_QUOTE = "blockquote";
    private static final String TAG_FORMAT_BAR_BUTTON_UL = "unorderedList";
    private static final String TAG_FORMAT_BAR_BUTTON_OL = "orderedList";
    private static final String TAG_FORMAT_BAR_BUTTON_LINK = "link";
    private static final String TAG_FORMAT_BAR_BUTTON_STRIKETHROUGH = "strikeThrough";

    private static final float TOOLBAR_ALPHA_ENABLED = 1;
    private static final float TOOLBAR_ALPHA_DISABLED = 0.5f;

    private String mParamTitle;
    private String mParamContent;

    private Activity mActivity;
    private EditorWebViewAbstract mWebView;

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
        mActivity = getActivity();
        if (getArguments() != null) {
            mParamTitle = getArguments().getString(ARG_PARAM_TITLE);
            mParamContent = getArguments().getString(ARG_PARAM_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        mWebView = (EditorWebViewAbstract) view.findViewById(R.id.webview);
        initJsEditor();

        ToggleButton mediaButton = (ToggleButton) view.findViewById(R.id.format_bar_button_media);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_MEDIA, mediaButton);

        ToggleButton boldButton = (ToggleButton) view.findViewById(R.id.format_bar_button_bold);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_BOLD, boldButton);

        ToggleButton italicButton = (ToggleButton) view.findViewById(R.id.format_bar_button_italic);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_ITALIC, italicButton);

        ToggleButton quoteButton = (ToggleButton) view.findViewById(R.id.format_bar_button_quote);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_QUOTE, quoteButton);

        ToggleButton ulButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ul);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_UL, ulButton);

        ToggleButton olButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ol);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_OL, olButton);

        ToggleButton linkButton = (ToggleButton) view.findViewById(R.id.format_bar_button_link);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_LINK, linkButton);

        // Tablet-only
        ToggleButton strikethroughButton = (ToggleButton) view.findViewById(R.id.format_bar_button_strikethrough);
        if (strikethroughButton != null) {
            mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_STRIKETHROUGH, strikethroughButton);
        }

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

    private void initJsEditor() {
        String htmlEditor = Utils.getHtmlFromFile(mActivity, "android-editor.html");

        mWebView.addJavascriptInterface(new JsCallbackReceiver(this), JS_CALLBACK_HANDLER);

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");

        enableWebDebugging(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.format_bar_button_bold) {
            mWebView.execJavaScriptFromString("ZSSEditor.setBold();");
        } else if (id == R.id.format_bar_button_italic) {
            mWebView.execJavaScriptFromString("ZSSEditor.setItalic();");
        } else if (id == R.id.format_bar_button_strikethrough) {
            mWebView.execJavaScriptFromString("ZSSEditor.setStrikeThrough();");
        } else if (id == R.id.format_bar_button_quote) {
            mWebView.execJavaScriptFromString("ZSSEditor.setBlockquote();");
        } else if (id == R.id.format_bar_button_ul) {
            mWebView.execJavaScriptFromString("ZSSEditor.setUnorderedList();");
        } else if (id == R.id.format_bar_button_ol) {
            mWebView.execJavaScriptFromString("ZSSEditor.setOrderedList();");
        } else if (id == R.id.format_bar_button_media) {
            // TODO: Handle inserting media
            ((ToggleButton) v).setChecked(false);
        } else if (id == R.id.format_bar_button_link) {
            // TODO: Handle inserting a link
            ((ToggleButton) v).setChecked(false);
        } else if (id == R.id.format_bar_button_html) {
            // TODO: Handle HTML mode toggling
            ((ToggleButton) v).setChecked(false);
        }
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
        // TODO
    }

    @Override
    public void setContent(CharSequence text) {
        // TODO
    }

    @Override
    public CharSequence getTitle() {
        // TODO
        return null;
    }

    @Override
    public CharSequence getContent() {
        // TODO
        return null;
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

    public void onDomLoaded() {
        mWebView.post(new Runnable() {
            public void run() {
                String title = "I'm editing a post!";
                String contentHtml = Utils.getHtmlFromFile(mActivity, "example-content.html");

                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setMultiline('true');");

                // Load example content into editor
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_title').setHTML('" +
                        Utils.escapeHtml(title) + "');");
                mWebView.execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setHTML('" +
                        Utils.escapeHtml(contentHtml) + "');");
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
        final String id = selectionArgs.get("id"); // The field currently in focus
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (!id.isEmpty()) {
                    switch(id) {
                        case "zss_field_title":
                            updateToolbarEnabledState(false);
                            break;
                        case "zss_field_content":
                            updateToolbarEnabledState(true);
                            break;
                    }
                }
            }
        });
    }

    void updateToolbarEnabledState(boolean enabled) {
        float alpha = (enabled ? TOOLBAR_ALPHA_ENABLED : TOOLBAR_ALPHA_DISABLED);
        for(ToggleButton button : mTagToggleButtonMap.values()) {
            button.setEnabled(enabled);
            button.setAlpha(alpha);
        }
    }
}
