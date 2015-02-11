package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EditorFragment extends EditorFragmentAbstract {
    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private String mParamTitle;
    private String mParamContent;
    private WebView mWebView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        mWebView = (WebView) view.findViewById(R.id.webview);
        initWebView();
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
            public boolean onConsoleMessage(ConsoleMessage cm) {
                AppLog.e(T.EDITOR, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                AppLog.e(T.EDITOR, message);
                return true;
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceId) {
                AppLog.e(T.EDITOR, message + " -- from line " + lineNumber + " of " + sourceId);
            }
        });
        String htmlEditor = getHtmlEditor();
        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");
    }

    private String getStringFromAsset(String filename) throws IOException {
        if (!isAdded()) {
            return null;
        }
        AssetManager assetManager = getActivity().getAssets();
        InputStream in = assetManager.open(filename);
        InputStreamReader is = new InputStreamReader(in);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();
        while (read != null) {
            sb.append(read);
            sb.append('\n');
            read = br.readLine();
        }
        return sb.toString();
    }

    private String getHtmlEditor() {
        try {
            return getStringFromAsset("android-editor.html");
        } catch (IOException e) {
            AppLog.e(T.EDITOR, e.getMessage());
            return null;
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
    public void appendMediaFile(MediaFile mediaFile) {
        // TODO
    }
}
