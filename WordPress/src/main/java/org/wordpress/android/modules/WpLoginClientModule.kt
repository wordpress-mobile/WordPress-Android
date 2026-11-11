package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpRsOkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.wpRsErrorLogger
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpLoginClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.WpRequestErrorLogPolicy
import uniffi.wp_api.WpRequestUrlLogDetail
import uniffi.wp_api.WpResponseBodyLogDetail

/**
 * How a [WpLoginClient] is configured, in one place, so every API discovery the app runs reaches
 * the network the same way and reports a failure under the same policy.
 */
@InstallIn(SingletonComponent::class)
@Module
class WpLoginClientModule {
    /**
     * Unscoped, as [WpComApiClientModule] is: the client holds its collaborators and little else,
     * and the transport behind it is shared.
     *
     * Discovery runs before the app has any relationship with the site, so it logs at a stricter
     * policy than the other clients.
     */
    @Provides
    fun provideWpLoginClient(
        @WpRsOkHttpClient okHttpClient: OkHttpClient,
        networkAvailabilityProvider: WpNetworkAvailabilityProvider,
    ): WpLoginClient = WpLoginClient(
        requestExecutor = WpRequestExecutor(
            httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
            networkAvailabilityProvider = networkAvailabilityProvider
        ),
        errorLogger = wpRsErrorLogger(
            WpRequestErrorLogPolicy(
                requestUrl = WpRequestUrlLogDetail.QUERY_KEYS_ONLY,
                responseBody = WpResponseBodyLogDetail.OMITTED
            )
        )
    )
}
