package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.text.TextUtils;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
 * username and password of the blog configured for this activity.
 */
public class WPWebViewClient extends WebViewClient {
    private final Blog mBlog;
    private String mToken;

    public WPWebViewClient(Blog blog) {
        super();
        this.mBlog = blog;
        mToken = AccountHelper.getDefaultAccount().getAccessToken();
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
    public WebResourceResponse shouldInterceptRequest(WebView view, String stringUrl) {
        // Intercept requests for private images and add the WP.com authorization header
        if (mBlog != null && mBlog.isPrivate() && !TextUtils.isEmpty(mToken) && UrlUtils.isImageUrl(stringUrl)) {
            try {
                URL url = new URL(stringUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Bearer " + mToken);
                urlConnection.setReadTimeout(WPRestClient.REST_TIMEOUT_MS);
                urlConnection.setConnectTimeout(WPRestClient.REST_TIMEOUT_MS);
                WebResourceResponse response = new WebResourceResponse(urlConnection.getContentType(),
                        urlConnection.getContentEncoding(),
                        urlConnection.getInputStream());
                return response;
            } catch (ClassCastException e) {
                AppLog.e(AppLog.T.POSTS, "Invalid connection type - URL: " + stringUrl);
            } catch (MalformedURLException e) {
                AppLog.e(AppLog.T.POSTS, "Malformed URL: " + stringUrl);
            } catch (IOException e) {
                AppLog.e(AppLog.T.POSTS, "Invalid post detail request: " + e.getMessage());
            }
        }
        return super.shouldInterceptRequest(view, stringUrl);
    }
}
