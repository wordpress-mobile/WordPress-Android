package org.wordpress.android.networking.rs

import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.ApiUrlResolver

/**
 * Supplies [WpApiClient] instances for a given [RsSite].
 *
 * The interface lives in this module so that callers do not have to
 * depend on fluxc. The implementation
 * (`WpApiClientProviderImpl` in the app module) owns caching, OAuth
 * token refresh, and cookie-nonce auth.
 *
 * Routing rules (proxy vs. direct-to-origin) are encoded in
 * [shouldUseWpComProxy] — see `docs/wpcom-routing.md`.
 */
interface WpApiClientProvider {
    /** Removes all cached API clients (e.g. on sign-out). */
    fun clearAllClients()

    /**
     * Removes the cached self-hosted client for a specific site so
     * that the next call to [getWpApiClient] creates a fresh client
     * with up-to-date credentials.
     */
    fun clearSelfHostedClient(siteId: Int)

    fun getWpApiClient(
        site: RsSite,
        uploadListener: WpRequestExecutor.UploadListener? = null
    ): WpApiClient

    fun getWpApiClientCookiesNonceAuthentication(site: RsSite): WpApiClient

    fun getApiUrlResolver(site: RsSite): ApiUrlResolver

    fun getApiRootUrlFrom(site: RsSite): String
}
