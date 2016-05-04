
package org.wordpress.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import org.wordpress.android.R;

import java.util.Map;

/**
 * Basic activity for displaying a WebView.
 */
public abstract class WebViewActivity extends AppCompatActivity {
    /** Primary webview used to display content. */

    private static final String URL = "url";

    protected WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);

        // clear title text so there's no title until actual web page title can be shown
        // this is done here rather than in the manifest to automatically handle descendants
        // such as AuthenticatedWebViewActivity
        setTitle("");

        configureView();

        // note: do NOT call mWebView.getSettings().setUserAgentString(WordPress.getUserAgent())
        // here since it causes problems with the browser-sniffing that some sites rely on to
        // format the page for mobile display
        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        configureWebView();

        if (savedInstanceState == null) {
            loadContent();
        }
    }

    /*
     * load the desired content - only done on initial activity creation (ie: when savedInstanceState
     * is null) since onSaveInstanceState() and onRestoreInstanceState() will take care of saving
     * and restoring the correct URL when the activity is recreated - note that descendants should
     * override this w/o calling super() to load a different URL.
     */
    protected void loadContent() {
        String url = getIntent().getStringExtra(URL);
        if (url != null) {
            loadUrl(url);
        }
    }

    /*
     * save the webView state with the bundle so it can be restored
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mWebView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    /*
     * restore the webView state saved above
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
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

    public void configureView() {
        setContentView(R.layout.webview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /*
     * descendants should override this to set a WebViewClient, WebChromeClient, and anything
     * else necessary to configure the webView prior to navigation
     */
    protected void configureWebView() {
        // noop
    }

    private void pauseWebView() {
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    private void resumeWebView() {
        if (mWebView != null) {
            mWebView.onResume();
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

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        mWebView.loadUrl(url, additionalHttpHeaders);
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
