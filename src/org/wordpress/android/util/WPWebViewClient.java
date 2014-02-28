package org.wordpress.android.util;

import android.net.http.SslError;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.datasets.TrustedSslDomainTable;
import org.wordpress.android.models.Blog;

/**
 * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
 * username and password of the blog configured for this activity.
 */
public class WPWebViewClient extends WebViewClient {
    private Blog blog;

    public WPWebViewClient(Blog blog) {
        super();
        this.blog = blog;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Found a bug on some pages where there is an incorrect
        // auto-redirect to file:///android_asset/webkit/.
        if (!url.equals("file:///android_asset/webkit/")) {
            view.loadUrl(url);
        }
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {

    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        handler.proceed(blog.getHttpuser(), blog.getHttppassword());
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        String domain = UrlUtils.getDomainFromUrl(error.getUrl());
        if (TrustedSslDomainTable.isDomainTrusted(domain)) {
            handler.proceed();
        } else {
            super.onReceivedSslError(view, handler, error);
        }
    }
}

