package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// TODO: use AppLog instead of Log
public class EditorActivity extends ActionBarActivity {
    WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WordPress-Editor", description);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.e("WordPress-Editor", cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.e("WordPress-Editor", message);
                return true;
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceId) {
                Log.e("WordPress-Editor", message + " -- from line " + lineNumber + " of " + sourceId);
            }
        });
        String htmlEditor = getHtmlEditor();
        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");
    }

    private String getStringFromAsset(String filename) throws IOException {
        AssetManager assetManager = getAssets();
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
            Log.e("WordPress-Editor", e.getMessage());
            return null;
        }
    }
}
