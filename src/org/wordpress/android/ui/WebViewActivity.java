
package org.wordpress.android.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
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

}
