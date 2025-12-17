package org.wordpress.android.posttypes.bridge

import uniffi.wp_mobile.WpSelfHostedService

/**
 * Factory interface for creating [WpSelfHostedService] instances.
 *
 * This interface is defined in the posttypes module but implemented in the main app module.
 * This allows the posttypes module to remain isolated from FluxC while still accessing
 * properly configured wordpress-rs services.
 *
 * ## Implementation Notes
 * The main app module provides an implementation via Hilt that:
 * - Looks up site credentials from SiteStore
 * - Creates the appropriate authentication provider
 * - Configures request executors with interceptors
 * - Uses the singleton [WpApiCache] instance
 *
 * @see SiteReference for the site data passed to this factory
 */
interface WpSelfHostedServiceFactory {
    /**
     * Creates a [WpSelfHostedService] for the given site.
     *
     * @param localSiteId The local database site ID used to look up credentials
     * @param siteUrl The site's base URL (e.g., "https://example.com")
     * @return A configured [WpSelfHostedService] ready to make API requests
     */
    fun create(localSiteId: Int, siteUrl: String): WpSelfHostedService
}
