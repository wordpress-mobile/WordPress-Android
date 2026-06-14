package org.wordpress.android.networking.restapi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.createWpComAuthProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.RequestErrorLogger
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val READ_WRITE_TIMEOUT_SECONDS = 60L

internal const val WP_COM_OK_HTTP_CLIENT = "wpcom-rs-okhttp"

/**
 * Provides a single, app-wide [WpComApiClient] for the `public-api.wordpress.com` REST API.
 *
 * The client is stateless: it reads the WP.com bearer token live on every request via
 * [createWpComAuthProvider], so one shared instance transparently survives token refresh and
 * sign-out/sign-in. The shared [OkHttpClient] is where the real reuse matters (one connection and
 * thread pool app-wide); it is configured specifically for this stack rather than reusing the
 * legacy networking clients.
 */
@Module
@InstallIn(SingletonComponent::class)
object WpComApiClientModule {
    @Provides
    @Singleton
    @Named(WP_COM_OK_HTTP_CLIENT)
    fun provideWpComOkHttpClient(
        trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(trackNetworkRequestsInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideWpComApiClient(
        @Named(WP_COM_OK_HTTP_CLIENT) okHttpClient: OkHttpClient,
        networkAvailabilityProvider: WpNetworkAvailabilityProvider,
        accountStore: AccountStore,
    ): WpComApiClient = WpComApiClient(
        requestExecutor = WpRequestExecutor(
            httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
            networkAvailabilityProvider = networkAvailabilityProvider,
        ),
        authProvider = createWpComAuthProvider(accountStore),
        errorLogger = RequestErrorLogger { AppLog.e(AppLog.T.API, it) },
    )
}
