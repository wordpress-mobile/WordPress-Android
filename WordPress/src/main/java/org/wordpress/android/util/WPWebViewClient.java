package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

/**
 * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
 * username and password of the blog configured for this activity.
 */
public class WPWebViewClient extends WebViewClient {
    private final Blog mBlog;
    private String mToken;

    public WPWebViewClient(Context context, Blog blog) {
        super();
        this.mBlog = blog;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mToken = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, "");
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
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        if (mBlog != null && mBlog.hasValidHTTPAuthCredentials()) {
            // Check that the HTTP AUth protected domain is the same of the blog. Do not send current blog's HTTP
            // AUTH credentials to external site.
            // NOTE: There is still a small security hole here, since the realm is not considered when getting
            // the password. Unfortunately the real is not stored when setting up the blog, and we cannot compare it
            // at this point.
            String domainFromHttpAuthRequest = UrlUtils.getDomainFromUrl(UrlUtils.addUrlSchemeIfNeeded(host, false));
            String currentBlogDomain = UrlUtils.getDomainFromUrl(mBlog.getUrl());
            if (domainFromHttpAuthRequest.equals(currentBlogDomain)) {
                handler.proceed(mBlog.getHttpuser(), mBlog.getHttppassword());
                return;
            }
        }
        // TODO: If there is no match show the HTTP Auth dialog here. Like a normal browser usually does...
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        try {
            if (SelfSignedSSLCertsManager.getInstance(view.getContext()).isCertificateTrusted(error.getCertificate())) {
                handler.proceed();
                return;
            }
        } catch (GeneralSecurityException e) {
            // Do nothing
        } catch (IOException e) {
            // Do nothing
        }

        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        // Intercept requests for private images and add the WP.com authorization header
        if (mBlog != null && mBlog.isPrivate() && !TextUtils.isEmpty(mToken) && UrlUtils.isImageUrl(url)) {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + mToken);
            try {
                HttpResponse httpResponse = client.execute(httpGet);
                InputStream responseInputStream = httpResponse.getEntity().getContent();
                return new WebResourceResponse(httpResponse.getEntity().getContentType().toString(),
                        "UTF-8", responseInputStream);
            } catch (IOException e) {
                AppLog.e(AppLog.T.POSTS, "Invalid post detail request: " + e.getMessage());
            }
        }

        return super.shouldInterceptRequest(view, url);
    }
}
