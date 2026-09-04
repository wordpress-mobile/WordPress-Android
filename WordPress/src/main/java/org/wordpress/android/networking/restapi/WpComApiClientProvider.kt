package org.wordpress.android.networking.restapi

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.applyWpRsTimeouts
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.DEFAULT_REQUEST_ERROR_LOG_POLICY
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestErrorLogger
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.api.kotlin.fromLocale
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpComLanguage
import uniffi.wp_api.WpComLanguageProvider
import javax.inject.Inject

class WpComApiClientProvider @Inject constructor(
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
    private val localeManagerWrapper: LocaleManagerWrapper,
) {
    /**
     * WordPress.com localizes a response when the request carries a locale query parameter. The
     * parameter name varies by API version, so it can't live in an endpoint's params type; instead
     * the library appends it to every WP.com request, asking this provider once per request.
     * Returning null sends no locale and leaves the choice to the server.
     */
    private val languageProvider = object : WpComLanguageProvider {
        override fun currentLanguage(): WpComLanguage? =
            WpComLanguage.fromLocale(localeManagerWrapper.getLocale())
    }

    /**
     * Logs request failures through the library's redaction policy. The default policy keeps the
     * request path and omits the response body, which is the guarantee the removed
     * WpRequestResult.toLogErrorString() used to give at each call site.
     */
    private val errorLogger = WpRequestErrorLogger(DEFAULT_REQUEST_ERROR_LOG_POLICY) { message ->
        AppLog.e(AppLog.T.API, message)
    }

    /**
     * [interceptors] are installed on the underlying OkHttp client. Callers that need request
     * inspection (e.g. the network-request tracker) pass their own; every client built here gets
     * the shared timeouts, error logger and language provider either way.
     */
    fun getWpComApiClient(
        accessToken: String,
        interceptors: List<Interceptor> = emptyList(),
    ): WpComApiClient {
        val okHttpClient = OkHttpClient.Builder()
            .applyWpRsTimeouts()
            .apply { interceptors.forEach { addInterceptor(it) } }
            .build()

        return WpComApiClient(
            requestExecutor = WpRequestExecutor(
                httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
            authProvider = WpAuthenticationProvider.staticWithAuth(WpAuthentication.Bearer(token = accessToken)
            ),
            errorLogger = errorLogger,
            languageProvider = languageProvider
        )
    }
}
