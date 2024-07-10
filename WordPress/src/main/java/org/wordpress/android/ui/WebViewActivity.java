package org.wordpress.android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.util.extensions.CompatExtensionsKt;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Basic activity for displaying a WebView.
 */
public abstract class WebViewActivity extends LocaleAwareActivity {
    /**
     * Primary webview used to display content.
     */

    private static final String URL = "url";

    private static final String WEBVIEW_CHROMIUM_STATE = "WEBVIEW_CHROMIUM_STATE";
    private static final int WEBVIEW_CHROMIUM_STATE_THRESHOLD = 300 * 1024; // 300 KB

    protected WebView mWebView;

    @Inject UserAgent mUserAgent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);

        // clear title text so there's no title until actual web page title can be shown
        // this is done here rather than in the manifest to automatically handle descendants
        // such as AuthenticatedWebViewActivity
        setTitle("");

        configureView();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    cancel();
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        // Setting this user agent makes Calypso sites hide any WordPress UIs (e.g. Masterbar, banners, etc.).
        if (mUserAgent != null) {
            mWebView.getSettings().setUserAgentString(mUserAgent.toString());
        }
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mWebView.saveState(outState);

        // If the WebView state is too large, remove it from the bundle and track the error. This workaround is
        // necessary since the Android system cannot handle large states without a crash.
        // Note that Chromium `WebViewBrowserFragment` uses a similar workaround for this issue:
        // https://source.chromium.org/chromium/chromium/src/+/27a9bbd3dcd7005ac9f3862dc2e356b557023de9
        byte[] webViewState = outState.getByteArray(WEBVIEW_CHROMIUM_STATE);
        if (webViewState != null && webViewState.length > WEBVIEW_CHROMIUM_STATE_THRESHOLD) {
            outState.remove(WEBVIEW_CHROMIUM_STATE);

            // Save the URL so it can be restored later
            String url = mWebView.getUrl();
            outState.putString(URL, url);

            // Track the error to better understand the root of the issue
            Map<String, String> properties = new HashMap<>();
            properties.put(URL, url);
            AnalyticsTracker.track(AnalyticsTracker.Stat.WEBVIEW_TOO_LARGE_PAYLOAD_ERROR, properties);
        }
        super.onSaveInstanceState(outState);
    }

    /*
     * restore the webView state saved above
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(WEBVIEW_CHROMIUM_STATE)) {
            mWebView.restoreState(savedInstanceState);
        } else {
            String url = savedInstanceState.getString(URL);
            if (url != null) {
                mWebView.loadUrl(url);
            }
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

    public void configureView() {
        setContentView(R.layout.webview);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
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
            if (isFinishing()) {
                loadUrl("about:blank");
            }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            cancel();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void cancel() {
        // nop
    }
}
