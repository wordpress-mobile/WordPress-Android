package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class ReleaseOkHttpClientModule {
    @Provides
    @Named("regular")
    public OkHttpClient.Builder provideOkHttpClient() {
        return new OkHttpClient.Builder();
    }

    @Provides
    @Named("custom-ssl")
    public OkHttpClient.Builder provideOkHttpClientCustomSSL(MemorizingTrustManager memorizingTrustManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            AppLog.e(T.API, e);
        }
        return builder;
    }
}
