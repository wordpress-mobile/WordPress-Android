package org.wordpress.android.util.helpers;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class WPWebChromeClient extends WebChromeClient {
    private final ProgressBar mProgressBar;
    private final Activity mActivity;
    private final boolean mAutoUpdateActivityTitle;

    public WPWebChromeClient(Activity activity, ProgressBar progressBar) {
        mActivity = activity;
        mProgressBar = progressBar;
        mAutoUpdateActivityTitle = true;
    }

    public WPWebChromeClient(Activity activity,
                             ProgressBar progressBar,
                             boolean autoUpdateActivityTitle) {
        mActivity = activity;
        mProgressBar = progressBar;
        mAutoUpdateActivityTitle = autoUpdateActivityTitle;
    }

    public void onProgressChanged(WebView webView, int progress) {
        if (mActivity != null
                && !mActivity.isFinishing()
                && mAutoUpdateActivityTitle
                && !TextUtils.isEmpty(webView.getTitle())) {
            mActivity.setTitle(webView.getTitle());
        }
        if (mProgressBar != null) {
            if (progress == 100) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(progress);
            }
        }
    }
}
