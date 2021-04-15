package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.net.CookieManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;

@Module
public class ReleaseOkHttpClientModule {

    @Provides
    @Named("regular")
    public OkHttpClient.Builder provideOkHttpClientBuilder(final CookieJar cookieJar) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(cookieJar);
        return builder;
    }

    @Provides
    @Named("no-redirects")
    public OkHttpClient.Builder provideNoRedirectsOkHttpClientBuilder(final CookieJar cookieJar) {
        OkHttpClient.Builder builder = provideOkHttpClientBuilder(cookieJar);
        builder.followRedirects(false);
        return builder;
    }

    @Provides
    @Named("custom-ssl")
    public OkHttpClient.Builder provideOkHttpClientBuilderCustomSSL(final MemorizingTrustManager memorizingTrustManager,
                                                                    final CookieJar cookieJar) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(cookieJar);
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.hostnameVerifier(memorizingTrustManager.wrapHostnameVerifier(OkHostnameVerifier.INSTANCE));
            builder.sslSocketFactory(sslSocketFactory, memorizingTrustManager);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            AppLog.e(T.API, e);
        }
        return builder;
    }

    @Singleton
    @Provides
    @Named("custom-ssl")
    public OkHttpClient provideMediaOkHttpClientInstanceCustomSSL(@Named("custom-ssl") OkHttpClient.Builder builder) {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    @Singleton
    @Provides
    @Named("regular")
    public OkHttpClient provideMediaOkHttpClientInstance(@Named("regular") OkHttpClient.Builder builder) {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }
}
