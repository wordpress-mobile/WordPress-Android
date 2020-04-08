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

@Module
public class ReleaseOkHttpClientModule {
    private static CookieJar mCookieJar = new JavaNetCookieJar(new CookieManager());

    @Provides
    @Named("regular")
    public OkHttpClient.Builder provideOkHttpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(mCookieJar);
        return builder;
    }

    @Provides
    @Named("custom-ssl")
    public OkHttpClient.Builder provideOkHttpClientBuilderCustomSSL(MemorizingTrustManager memorizingTrustManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(mCookieJar);
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory);
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
