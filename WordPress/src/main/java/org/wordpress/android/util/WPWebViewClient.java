package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.text.TextUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.util.SelfSignedSSLUtils.sslCertificateToX509;

/**
 * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
 * username and password of the blog configured for this activity.
 */
public class WPWebViewClient extends URLFilteredWebViewClient {
    /**
     * Timeout in milliseconds for read / connect timeouts
     */
    private static final int TIMEOUT_MS = 30000;

    private final SiteModel mSite;
    private String mToken;
    @Inject protected MemorizingTrustManager mMemorizingTrustManager;

    public WPWebViewClient(SiteModel site, String token) {
        this(site, token, null);
    }

    public WPWebViewClient(SiteModel site, String token, List<String> urls) {
        super(urls);
        ((WordPress) WordPress.getContext().getApplicationContext()).component().inject(this);
        mSite = site;
        mToken = token;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }


    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        X509Certificate certificate = sslCertificateToX509(error.getCertificate());
        if (certificate != null && mMemorizingTrustManager.isCertificateAccepted(certificate)) {
            handler.proceed();
            return;
        }

        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String stringUrl) {
        URL imageUrl = null;
        if (mSite != null && mSite.isPrivate() && UrlUtils.isImageUrl(stringUrl)) {
            try {
                imageUrl = new URL(UrlUtils.makeHttps(stringUrl));
            } catch (MalformedURLException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        // Intercept requests for private images and add the WP.com authorization header
        if (imageUrl != null
            && WPUrlUtils.safeToAddWordPressComAuthToken(imageUrl)
            && !TextUtils.isEmpty(mToken)) {
            try {
                // Force use of HTTPS for the resource, otherwise the request will fail for private sites
                HttpURLConnection urlConnection = (HttpURLConnection) imageUrl.openConnection();
                urlConnection.setRequestProperty("Authorization", "Bearer " + mToken);
                urlConnection.setReadTimeout(TIMEOUT_MS);
                urlConnection.setConnectTimeout(TIMEOUT_MS);
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
