package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.util.AppLog;

/**
 * Activity for opening stats external links in a webview.
 */
public class StatsWebViewActivity extends AuthenticatedWebViewActivity {
    public static final String STATS_AUTHENTICATED_URL = "stats_authenticated_url";
    public static final String STATS_URL = "stats_url";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setUserAgentString(WordPress.getUserAgent());
        mWebView.getSettings().setDomStorageEnabled(true);

        if (extras != null) {
            if (extras.containsKey(STATS_AUTHENTICATED_URL)) {
                String addressToLoad = extras.getString(STATS_AUTHENTICATED_URL);
                this.loadAuthenticatedUrl(addressToLoad);
            } else if (extras.containsKey(STATS_URL)) {
                String addressToLoad = extras.getString(STATS_URL);
                this.loadUrl(addressToLoad);
            }
        } else {
            AppLog.e(AppLog.T.UTILS, "No valid URL passed to the StatsWebViewActivity");
        }
    }
}
