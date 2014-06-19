package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Activity for opening stats external links in a webview.
 */
public class StatsWebViewActivity extends AuthenticatedWebViewActivity {
    public static final String STATS_AUTHENTICATED_URL = "stats_authenticated_url";
    public static final String STATS_AUTHENTICATED_USER = "stats_authenticated_user";
    public static final String STATS_AUTHENTICATED_PASSWD = "stats_authenticated_passwd";
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
                String username = extras.getString(STATS_AUTHENTICATED_USER, "");
                String password = extras.getString(STATS_AUTHENTICATED_PASSWD, "");
                this.loadAuthenticatedStatsUrl(addressToLoad, username, password);
            } else if (extras.containsKey(STATS_URL)) {
                String addressToLoad = extras.getString(STATS_URL);
                this.loadUrl(addressToLoad);
            }
        } else {
            AppLog.e(AppLog.T.STATS, "No valid URL passed to StatsWebViewActivity!!");
        }
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     *
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedStatsUrl(String url, String username, String passwd) {
        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(username, "UTF-8"), URLEncoder.encode(passwd, "UTF-8"),
                    URLEncoder.encode(url, "UTF-8"));
            mWebView.postUrl("https://wordpress.com/wp-login.php", postData.getBytes());
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
    }

    @Override
    protected void onDestroy() {
        // The 2 lines below fix an issue where this activity has leaked window
        // android.widget.ZoomButtonsController$Container
        mWebView.getSettings().setBuiltInZoomControls(false);
        mWebView.setVisibility(View.GONE);
        super.onDestroy();
    }
}