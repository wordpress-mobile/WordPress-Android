package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPWebChromeClient;
import org.wordpress.passcodelock.AppLockManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Activity for opening stats external links in a webview.
 */
public class StatsWebViewActivity extends WebViewActivity {
    public static final String STATS_AUTHENTICATED_URL = "stats_authenticated_url";
    public static final String STATS_AUTHENTICATED_USER = "stats_authenticated_user";
    public static final String STATS_AUTHENTICATED_PASSWD = "stats_authenticated_passwd";
    public static final String STATS_URL = "stats_url";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WPWebChromeClient(this, (ProgressBar) findViewById(R.id.progress_bar)));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null)
            return false;

        int itemID = item.getItemId();
        if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        } else if (itemID == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(share, getResources().getText(R.string.share_link)));
            return true;
        } else if (itemID == R.id.menu_browser) {
            String url = mWebView.getUrl();
            if (url != null) {
                Uri uri = Uri.parse(url);
                if (uri != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                    AppLockManager.getInstance().setExtendedTimeout();
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
