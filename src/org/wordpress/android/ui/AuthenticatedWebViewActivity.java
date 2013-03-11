
package org.wordpress.android.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.os.Bundle;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

/**
 * Activity for displaying WordPress content in a webview which may require authentication.
 * Currently, this activity can only load content for the {@link WordPress.currentBlog}.
 */
public class AuthenticatedWebViewActivity extends WebViewActivity {

    /**
     * Blog for which this activity is loading content.
     */
    protected Blog mBlog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlog = WordPress.getCurrentBlog();
        if (mBlog == null) {
            Toast.makeText(this, getResources().getText(R.string.blog_not_found),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        mWebView.setWebViewClient(new WordPressWebViewClient(mBlog));

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSavePassword(false);
    }

    /**
     * Get the URL of the WordPress login page.
     * 
     * @return URL of the login page.
     */
    protected String getLoginUrl() {
        if (mBlog.getUrl().lastIndexOf("/") != -1) {
            return mBlog.getUrl().substring(0, mBlog.getUrl().lastIndexOf("/"))
                    + "/wp-login.php";
        } else {
            return mBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
        }
    }

    /**
     * Login to the WordPress blog and load the specified URL.
     * 
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedUrl(String url) {
        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    mBlog.getUsername(), mBlog.getPassword(),
                    URLEncoder.encode(url, "UTF-8"));
            mWebView.postUrl(getLoginUrl(), postData.getBytes());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
     * username and password of the blog configured for this activity.
     */
    private class WordPressWebViewClient extends WebViewClient {
        private Blog blog;

        WordPressWebViewClient(Blog blog) {
            super();
            this.blog = blog;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            handler.proceed(blog.getHttpuser(), blog.getHttppassword());
        }
    }

    /**
     * WebChromeClient that displays "Loading..." title until the content of the webview is fully
     * loaded.
     */
    protected class WordPressWebChromeClient extends WebChromeClient {
        private Context context;
        private CharSequence title;

        public WordPressWebChromeClient(Context context, CharSequence title) {
            this.context = context;
            this.title = title;
        }

        public void onProgressChanged(WebView view, int progress) {
            AuthenticatedWebViewActivity.this.setTitle(
                    context.getResources().getText(R.string.loading));
            AuthenticatedWebViewActivity.this.setProgress(progress * 100);

            if (progress == 100) {
                AuthenticatedWebViewActivity.this.setTitle(title);
            }
        }
    }
}
