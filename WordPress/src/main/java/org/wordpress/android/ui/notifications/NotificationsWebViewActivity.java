package org.wordpress.android.ui.notifications;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;

@SuppressLint("SetJavaScriptEnabled")
public class NotificationsWebViewActivity extends AuthenticatedWebViewActivity {
    public static final String URL_TO_LOAD = "external_url";

    public static void openUrl(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url))
            return;
        Intent intent = new Intent(context, NotificationsWebViewActivity.class);
        intent.putExtra(NotificationsWebViewActivity.URL_TO_LOAD, url);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setDomStorageEnabled(true);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // load URL if one was provided in the intent
        String url = getIntent().getStringExtra(URL_TO_LOAD);
        if (!TextUtils.isEmpty(url)) {
            loadAuthenticatedUrl(url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_signout).setVisible(false);
        menu.findItem(R.id.menu_settings).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
