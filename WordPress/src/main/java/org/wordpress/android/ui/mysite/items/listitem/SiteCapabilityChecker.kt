package org.wordpress.android.ui.mysite.items.listitem

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.UserCapability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks user capabilities for a site using the WordPress REST API via wordpress-rs.
 * Results are cached per site to avoid redundant network requests. The full set of the
 * current user's capabilities is fetched once and the flags we care about are cached
 * together, so screens needing more than one capability don't trigger extra requests.
 */
@Singleton
class SiteCapabilityChecker @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper
) {
    // Keyed by the local site id (site.id), not site.siteId: the latter (the WP.com blog id) is 0
    // for every self-hosted application-password site, which would collide their capabilities.
    private val capabilityCache = mutableMapOf<Int, CapabilityCache>()

    /**
     * Checks if the current user has the edit_theme_options capability for the given site.
     * Results are cached per site.
     */
    suspend fun hasEditThemeOptionsCapability(site: SiteModel): Boolean =
        capabilities(site).hasEditThemeOptions

    /**
     * Checks if the current user has the moderate_comments capability for the given site — i.e.
     * whether they may approve/unapprove, spam, trash, delete or edit comments. Results are
     * cached per site.
     */
    suspend fun canModerateComments(site: SiteModel): Boolean =
        capabilities(site).canModerateComments

    private suspend fun capabilities(site: SiteModel): CapabilityCache {
        capabilityCache[site.id]?.let { return it }
        val fetched = fetchCapabilities(site)
        capabilityCache[site.id] = fetched
        return fetched
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchCapabilities(site: SiteModel): CapabilityCache {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.users().retrieveMeWithEditContext()
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val capabilities = response.response.data.capabilities
                    CapabilityCache(
                        hasEditThemeOptions = capabilities.hasCap(UserCapability.EditThemeOptions),
                        canModerateComments = capabilities.hasCap(UserCapability.ModerateComments)
                    )
                }
                // Fail closed: an unresolved capability disables the guarded action rather than
                // offering it and letting the server reject the request after the fact.
                else -> CapabilityCache()
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "Failed to fetch user capabilities: ${e.message}")
            CapabilityCache()
        }
    }

    /**
     * Clears the cached capabilities for a specific site.
     */
    fun clearCacheForSite(site: SiteModel) {
        capabilityCache.remove(site.id)
    }

    private data class CapabilityCache(
        val hasEditThemeOptions: Boolean = false,
        val canModerateComments: Boolean = false
    )
}
