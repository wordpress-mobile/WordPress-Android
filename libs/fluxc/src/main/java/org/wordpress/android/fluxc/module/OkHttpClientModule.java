package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

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
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;

@Module
public abstract class OkHttpClientModule {
    @Singleton
    @Provides
    @Named("no-cookies")
    public static OkHttpClient provideNoCookiesOkHttpClientBuilder(
            @Named("regular") final OkHttpClient okHttpRegularClient) {
        return okHttpRegularClient.newBuilder()
                                  .cookieJar(CookieJar.NO_COOKIES)
                                  .build();
    }

    @Provides
    @Named("no-redirects")
    public static OkHttpClient provideNoRedirectsOkHttpClientBuilder(
            @Named("custom-ssl") final OkHttpClient okHttpRegularClient) {
        return okHttpRegularClient.newBuilder()
                                  .followRedirects(false)
                                  .build();
    }

    @Singleton
    @Provides
    @Named("custom-ssl")
    public static OkHttpClient provideMediaOkHttpClientInstanceCustomSSL(
            @Named("regular") final OkHttpClient okHttpClient,
            final MemorizingTrustManager memorizingTrustManager) {
        final OkHttpClient.Builder builder = okHttpClient.newBuilder();
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.hostnameVerifier(memorizingTrustManager.wrapHostnameVerifier(OkHostnameVerifier.INSTANCE));
            builder.sslSocketFactory(sslSocketFactory, memorizingTrustManager);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            AppLog.e(T.API, e);
        }
        return builder.build();
    }

    @Multibinds abstract @Named("interceptors") Set<Interceptor> interceptorSet();

    @Multibinds abstract @Named("network-interceptors") Set<Interceptor> networkInterceptorSet();

    @Singleton
    @Provides
    @Named("regular")
    public static OkHttpClient provideMediaOkHttpClientInstance(
            final CookieJar cookieJar,
            @Named("interceptors") Set<Interceptor> interceptors,
            @Named("network-interceptors") Set<Interceptor> networkInterceptors) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }

        for (Interceptor networkInterceptor : networkInterceptors) {
            builder.addNetworkInterceptor(networkInterceptor);
        }

        return builder.cookieJar(cookieJar)
                      .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                      .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                      .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                      .build();
    }
}
