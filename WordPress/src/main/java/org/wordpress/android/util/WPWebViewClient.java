package org.wordpress.android.util;


import android.net.http.SslError;
import android.text.TextUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    @Inject @Named(OkHttpClientQualifiers.REGULAR) protected OkHttpClient mOkHttpClient;

    public WPWebViewClient(SiteModel site, String token, List<String> urls,
                           ErrorManagedWebViewClientListener listener) {
        super(urls, listener);
        ((WordPress) WordPress.getContext().getApplicationContext()).component().inject(this);
        mSite = site;
        mToken = token;
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
                Request request = new Request.Builder()
                        .url(imageUrl)
                        .addHeader("Authorization", "Bearer " + mToken)
                        .build();

                OkHttpClient client = mOkHttpClient.newBuilder()
                        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .build();

                Response okResponse = client.newCall(request).execute();
                if (okResponse.body() != null) {
                    return new WebResourceResponse(
                            okResponse.header("Content-Type"),
                            okResponse.header("Content-Encoding"),
                            okResponse.body().byteStream()
                    );
                }
            } catch (MalformedURLException e) {
                AppLog.e(AppLog.T.POSTS, "Malformed URL: " + stringUrl);
            } catch (IOException e) {
                AppLog.e(AppLog.T.POSTS, "Invalid post detail request: " + e.getMessage());
            }
        }
        return super.shouldInterceptRequest(view, stringUrl);
    }
}
