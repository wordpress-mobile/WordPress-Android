package org.wordpress.android.networking.restapi

import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val READ_WRITE_TIMEOUT = 60L
private const val CONNECT_TIMEOUT = 30L

class WpComApiClientProvider @Inject constructor(
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
) {
    fun getWpComApiClient(accessToken: String): WpComApiClient {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        return WpComApiClient(
            requestExecutor = WpRequestExecutor(
                httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
            authProvider = WpAuthenticationProvider.staticWithAuth(WpAuthentication.Bearer(token = accessToken)
            )
        )
    }
}
