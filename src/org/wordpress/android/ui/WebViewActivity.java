
package org.wordpress.android.ui;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;

import org.wordpress.android.Constants;
import org.wordpress.android.R;

/**
 * Basic activity for displaying a WebView.
 */
public class WebViewActivity extends SherlockActivity {
    /** Primary webview used to display content. */
    protected WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.webview);

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

}
