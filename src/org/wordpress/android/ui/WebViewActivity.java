
package org.wordpress.android.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import org.wordpress.android.Constants;
import org.wordpress.android.R;


/**
 * Basic activity for displaying a WebView.
 */
public class WebViewActivity extends WPActionBarActivity {
    /** Primary webview used to display content. */
    protected WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.webview);

        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayShowTitleEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.getSettings().setUserAgentString(Constants.USER_AGENT);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        // load URL if one was provided in the intent
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            loadUrl(url);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Flash video may keep playing if the webView isn't paused here
        pauseWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeWebView();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void pauseWebView() {
        if (mWebView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mWebView.onPause();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void resumeWebView() {
        if (mWebView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mWebView.onResume();
    }

    /**
     * Load the specified URL in the webview.
     * 
     * @param url URL to load in the webview.
     */
    protected void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {

        if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
