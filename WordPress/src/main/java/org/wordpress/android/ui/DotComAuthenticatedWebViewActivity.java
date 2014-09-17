package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPWebChromeClient;
import org.wordpress.passcodelock.AppLockManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Activity for opening stats external links in a webview.
 */
public class DotComAuthenticatedWebViewActivity extends WebViewActivity {
    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String URL_TO_LOAD = "url_to_load";

    public static void openUrlByUsingWPCOMCredentials(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url))
            return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        String authenticatedUser = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        String authenticatedPassword = WordPressDB.decryptPassword(
                settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
        );
        Intent intent = new Intent(context, DotComAuthenticatedWebViewActivity.class);
        intent.putExtra(DotComAuthenticatedWebViewActivity.AUTHENTICATION_USER, authenticatedUser);
        intent.putExtra(DotComAuthenticatedWebViewActivity.AUTHENTICATION_PASSWD, authenticatedPassword);
        intent.putExtra(DotComAuthenticatedWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(DotComAuthenticatedWebViewActivity.AUTHENTICATION_URL, "https://wordpress.com/wp-login.php");
        context.startActivity(intent);
    }

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
            String addressToLoad = extras.getString(URL_TO_LOAD);
            String username = extras.getString(AUTHENTICATION_USER, "");
            String password = extras.getString(AUTHENTICATION_PASSWD, "");
            String authURL = extras.getString(AUTHENTICATION_URL);
            this.loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        } else {
            AppLog.e(AppLog.T.STATS, "No valid parameters passed to StatsWebViewActivity!!");
        }
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     *
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String passwd) {
        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(username, "UTF-8"), URLEncoder.encode(passwd, "UTF-8"),
                    URLEncoder.encode(urlToLoad, "UTF-8"));
            mWebView.postUrl(authenticationURL, postData.getBytes());
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.STATS, e);
        }
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
        if (mWebView == null) {
            return false;
        }

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
