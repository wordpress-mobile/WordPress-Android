package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.HashMap;
import java.util.Map;

public class EditorFragment extends EditorFragmentAbstract implements View.OnClickListener, JsCallbackListener {
    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private static final String TAG_FORMAT_BAR_BUTTON_BOLD = "bold";

    private String mParamTitle;
    private String mParamContent;
    private EditorWebView mWebView;

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
        if (getArguments() != null) {
            mParamTitle = getArguments().getString(ARG_PARAM_TITLE);
            mParamContent = getArguments().getString(ARG_PARAM_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        mWebView = (EditorWebView) view.findViewById(R.id.webview);
        initWebView();

        ToggleButton boldButton = (ToggleButton) view.findViewById(R.id.bold);
        boldButton.setOnClickListener(this);
        mTagToggleButtonMap.put(TAG_FORMAT_BAR_BUTTON_BOLD, boldButton);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                AppLog.e(T.EDITOR, description);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(@NonNull ConsoleMessage cm) {
                AppLog.d(T.EDITOR, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                AppLog.d(T.EDITOR, message);
                return true;
            }
        });

        String htmlEditor = Utils.getHtmlFromFile(getActivity(), "android-editor.html");

        mWebView.addJavascriptInterface(new JsCallbackHandler(this), JS_CALLBACK_HANDLER);

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");

        enableWebDebugging(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bold) {
            mWebView.execJavaScriptFromString("ZSSEditor.setBold();");
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
                String contentHtml = Utils.getHtmlFromFile(getActivity(), "example-content.html");

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
}