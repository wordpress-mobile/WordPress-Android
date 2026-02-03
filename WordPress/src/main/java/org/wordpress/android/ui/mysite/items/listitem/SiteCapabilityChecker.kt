package org.wordpress.android.ui.mysite.items.listitem

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpRequestResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks user capabilities for a site using the WordPress REST API via wordpress-rs.
 * Results are cached per site to avoid redundant network requests.
 */
@Singleton
class SiteCapabilityChecker @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider
) {
    private val capabilityCache = mutableMapOf<Long, CapabilityCache>()

    /**
     * Checks if the current user has the edit_theme_options capability for the given site.
     * Results are cached per site ID.
     */
    suspend fun hasEditThemeOptionsCapability(site: SiteModel): Boolean {
        val siteId = site.siteId

        // Return cached value if available
        capabilityCache[siteId]?.let { cache ->
            return cache.hasEditThemeOptions
        }

        // Fetch from API and cache
        val hasCapability = fetchEditThemeOptionsCapability(site)
        capabilityCache[siteId] = CapabilityCache(hasEditThemeOptions = hasCapability)
        return hasCapability
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun fetchEditThemeOptionsCapability(site: SiteModel): Boolean {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.users().retrieveMeWithEditContext()
            }
            when (response) {
                is WpRequestResult.Success -> {
                    response.response.data.capabilities.entries.any { (key, value) ->
                        key.toString().contains("edit_theme_options", ignoreCase = true) && value
                    }
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Clears the cached capabilities for a specific site.
     */
    fun clearCacheForSite(siteId: Long) {
        capabilityCache.remove(siteId)
    }

    private data class CapabilityCache(
        val hasEditThemeOptions: Boolean
    )
}
