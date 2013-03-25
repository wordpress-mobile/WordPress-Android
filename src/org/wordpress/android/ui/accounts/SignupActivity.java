
package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class SignupActivity extends Activity {
    public Activity activity = this;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        webView = new WebView(this);

        setContentView(webView);
        setTitle(getResources().getText(R.string.new_account));

        setProgressBarIndeterminateVisibility(true);

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        webView.getSettings().setUserAgentString("wp-android/" + WordPress.versionName);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WordPressWebViewClient());
        webView.setWebChromeClient(new SignupWebChromeClient());
        webView.loadUrl("https://en.wordpress.com/signup/?ref=wp-android");

    }

    private class WordPressWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("wordpress://wpcom_signup_completed")) {
                if (url.indexOf("username=") > 0) {
                    String username = url.substring(url.indexOf("username=") + 9, url.length());
                    Bundle bundle = new Bundle();
                    bundle.putString("username", username);
                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish();
                }
            }
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.clearCache(true);
        }
    }

    /**
     * Updates progress loader
     */
    protected class SignupWebChromeClient extends WebChromeClient {

        public void onProgressChanged(WebView webView, int progress) {
            SignupActivity.this.setProgress(progress * 100);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed();
    }
}
