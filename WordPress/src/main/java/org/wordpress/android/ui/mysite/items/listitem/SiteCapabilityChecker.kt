package org.wordpress.android.ui.mysite.items.listitem

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.UserCapability
import java.util.concurrent.ConcurrentHashMap
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
    // Concurrent because it's a @Singleton read/written from multiple screens' ViewModels on the
    // multi-threaded default dispatcher.
    private val capabilityCache = ConcurrentHashMap<Int, CapabilityCache>()

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

    /**
     * The current user's WP user id for the given site, or null if it couldn't be resolved. Read
     * from the same "me" fetch as the capabilities above (no extra request). The rs comments list
     * uses it to tell the user's own replies apart when computing the Unreplied filter.
     */
    suspend fun currentUserId(site: SiteModel): Long? =
        capabilities(site).currentUserId

    private suspend fun capabilities(site: SiteModel): CapabilityCache {
        capabilityCache[site.id]?.let { return it }
        // Only a successful fetch is cached. On failure fall back to a fail-closed value for this
        // call but don't cache it, so a transient error doesn't disable the capability for the
        // whole session — the next call retries (the comments screens never clear this cache).
        return fetchCapabilities(site)?.also { capabilityCache[site.id] = it } ?: CapabilityCache()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchCapabilities(site: SiteModel): CapabilityCache? {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.users().retrieveMeWithEditContext()
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val user = response.response.data
                    CapabilityCache(
                        hasEditThemeOptions = user.capabilities.hasCap(UserCapability.EditThemeOptions),
                        canModerateComments = user.capabilities.hasCap(UserCapability.ModerateComments),
                        currentUserId = user.id
                    )
                }
                // Return null (not cached) so the next call retries rather than locking in a
                // fail-closed result from a transient error.
                else -> null
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "Failed to fetch user capabilities: ${e.message}")
            null
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
        val canModerateComments: Boolean = false,
        val currentUserId: Long? = null
    )
}
