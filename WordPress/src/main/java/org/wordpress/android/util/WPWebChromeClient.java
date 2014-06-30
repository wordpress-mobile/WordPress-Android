package org.wordpress.android.util;

import android.app.Activity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class WPWebChromeClient extends WebChromeClient {
    private ProgressBar mProgressBar;
    private Activity mActivity;

    public WPWebChromeClient(Activity activity, ProgressBar progressBar) {
        mProgressBar = progressBar;
        mActivity = activity;
    }

    public void onProgressChanged(WebView webView, int progress) {
        if (!mActivity.isFinishing()) {
            mActivity.setTitle(webView.getTitle());
        }
        if (progress == 100) {
            mProgressBar.setVisibility(View.GONE);
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(progress);
        }
    }
}