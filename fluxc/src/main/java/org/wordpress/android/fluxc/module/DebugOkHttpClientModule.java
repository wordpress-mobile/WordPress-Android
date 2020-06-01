package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.net.CookieManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.Multibinds;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

@Module
public abstract class DebugOkHttpClientModule {
    // These allow a library client to use this module without contributing any interceptors
    @Multibinds abstract @Named("interceptors") Set<Interceptor> interceptorSet();
    @Multibinds abstract @Named("network-interceptors") Set<Interceptor> networkInterceptorSet();

    private static CookieJar mCookieJar = new JavaNetCookieJar(new CookieManager());

    @Provides
    @Named("regular")
    public static OkHttpClient.Builder provideOkHttpClientBuilder(
            @Named("interceptors") Set<Interceptor> interceptors,
            @Named("network-interceptors") Set<Interceptor> networkInterceptors) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(mCookieJar);
        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }
        for (Interceptor networkInterceptor : networkInterceptors) {
            builder.addNetworkInterceptor(networkInterceptor);
        }
        return builder;
    }

    @Provides
    @Named("custom-ssl")
    public static OkHttpClient.Builder provideOkHttpClientBuilderCustomSSL(
            MemorizingTrustManager memorizingTrustManager,
            @Named("interceptors") Set<Interceptor> interceptors,
            @Named("network-interceptors") Set<Interceptor> networkInterceptors) {
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
        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }
        for (Interceptor networkInterceptor : networkInterceptors) {
            builder.addNetworkInterceptor(networkInterceptor);
        }
        return builder;
    }

    @Singleton
    @Provides
    @Named("custom-ssl")
    public static OkHttpClient provideMediaOkHttpClientInstanceCustomSSL(
            @Named("custom-ssl") OkHttpClient.Builder builder) {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    @Singleton
    @Provides
    @Named("regular")
    public static OkHttpClient provideMediaOkHttpClientInstance(
            @Named("regular") OkHttpClient.Builder builder) {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }
}
