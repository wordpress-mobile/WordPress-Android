package org.wordpress.android.networking.restapi

import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.applyWpRsTimeouts
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject

class WpComApiClientProvider @Inject constructor(
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
    private val languageProvider: AppWpComLanguageProvider,
    private val trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor,
) {
    fun getWpComApiClient(accessToken: String): WpComApiClient {
        val okHttpClient = OkHttpClient.Builder()
            .applyWpRsTimeouts()
            .addInterceptor(trackNetworkRequestsInterceptor)
            .build()

        return WpComApiClient(
            requestExecutor = WpRequestExecutor(
                httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
            authProvider = WpAuthenticationProvider.staticWithAuth(WpAuthentication.Bearer(token = accessToken)
            ),
            languageProvider = languageProvider
        )
    }
}
