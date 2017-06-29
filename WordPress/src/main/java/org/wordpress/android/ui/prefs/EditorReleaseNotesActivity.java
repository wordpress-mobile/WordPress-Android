package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.ui.WebViewActivity;

/**
 * Display release notes for editor.
 */
public class EditorReleaseNotesActivity extends WebViewActivity {
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        super.configureWebView();
    }

    @Override
    protected void loadContent() {
        loadUrl("https://make.wordpress.org/mobile/2017/04/08/introducing-the-aztec-mobile-editors/");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView.setWebViewClient(
            new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    EditorReleaseNotesActivity.this.setTitle(view.getTitle());
                }
            }
        );
    }
}
