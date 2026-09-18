package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.networking.restapi.WpComAuthenticationProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.DEFAULT_REQUEST_ERROR_LOG_POLICY
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestErrorLogger
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.api.kotlin.fromLocale
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpComLanguage
import uniffi.wp_api.WpComLanguageProvider

/**
 * How a [WpComApiClient] is configured, in one place, so every WordPress.com request the app makes
 * carries the same authentication, localization, error logging and request inspection.
 */
@InstallIn(SingletonComponent::class)
@Module
class WpComApiClientModule {
    /**
     * Unscoped, so each caller gets its own and releases it along with whatever held it. A client
     * stores its collaborators and little else — the uniffi request builder behind them is lazy,
     * and the transport it talks through is shared — so holding one per screen costs next to
     * nothing, and no caller inherits state from the one before it.
     */
    @Provides
    fun provideWpComApiClient(
        @WpRsOkHttpClient okHttpClient: OkHttpClient,
        authenticationProvider: WpComAuthenticationProvider,
        networkAvailabilityProvider: WpNetworkAvailabilityProvider,
        localeManagerWrapper: LocaleManagerWrapper,
    ): WpComApiClient = WpComApiClient(
        requestExecutor = WpRequestExecutor(
            httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient),
            networkAvailabilityProvider = networkAvailabilityProvider
        ),
        authProvider = WpAuthenticationProvider.dynamic(authenticationProvider),
        errorLogger = requestErrorLogger(),
        languageProvider = wpComLanguageProvider(localeManagerWrapper)
    )

    /** Logs request failures through the library's redaction policy. */
    private fun requestErrorLogger() =
        WpRequestErrorLogger(DEFAULT_REQUEST_ERROR_LOG_POLICY) { message ->
            AppLog.e(AppLog.T.API, message)
        }

    /**
     * WordPress.com localizes a response when the request carries a locale query parameter. The
     * parameter name varies by API version, so it can't live in an endpoint's params type; instead
     * the library appends it to every WP.com request, asking this provider once per request.
     * Returning null sends no locale and leaves the choice to the server.
     */
    private fun wpComLanguageProvider(localeManagerWrapper: LocaleManagerWrapper) =
        object : WpComLanguageProvider {
            override fun currentLanguage(): WpComLanguage? =
                WpComLanguage.fromLocale(localeManagerWrapper.getLocale())
        }
}
