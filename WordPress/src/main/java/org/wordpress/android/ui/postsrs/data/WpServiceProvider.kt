package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.createWpComAuthProvider
import org.wordpress.android.fluxc.store.AccountStore
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.cache.kotlin.DatabaseChangeNotifier
import rs.wordpress.cache.kotlin.WordPressApiCache
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_mobile.SiteInfo
import uniffi.wp_mobile.WpService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and caches [WpService] instances for both self-hosted WordPress sites (using application
 * passwords) and WordPress.com sites (using OAuth bearer tokens). Each service is configured with
 * the appropriate authentication and a shared SQLite-backed [WordPressApiCache], then keyed by
 * local site ID so subsequent requests reuse the same instance.
 */
@Singleton
class WpServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    private val accountStore: AccountStore,
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
) {
    private val services = mutableMapOf<Int, WpService>()
    private var cache: WordPressApiCache? = null

    @Synchronized
    fun getService(site: SiteModel): WpService {
        return services.getOrPut(site.id) { createService(site) }
    }

    /** Removes all cached services. */
    @Synchronized
    fun clearAll() {
        services.clear()
    }

    /**
     * Drops the cached service for a single site so the next [getService] rebuilds it. Call this
     * after the site's stored [SiteModel.getWpApiRestUrl] changes (e.g. a background heal), otherwise
     * a service built with the stale `apiRoot` keeps serving requests until the process restarts.
     */
    @Synchronized
    fun clearService(siteId: Int) {
        services.remove(siteId)
    }

    private fun createService(site: SiteModel): WpService {
        val delegate = createDelegate(site)
        val wpApiCache = getOrCreateCache()
        val siteInfo = if (site.isWPCom) {
            SiteInfo.WordPressCom(siteId = site.siteId.toULong())
        } else {
            SiteInfo.SelfHosted(
                siteUrl = ParsedUrl.parse(site.url),
                apiRoot = ParsedUrl.parse(resolveApiRoot(site)),
            )
        }
        return WpService(siteInfo, delegate, wpApiCache.cache)
    }

    /**
     * Resolves the REST API root for a [SiteInfo.SelfHosted] service.
     *
     * Atomic / Jetpack-WPCom-REST app-password sites store the WordPress.com REST *proxy* URL
     * (`https://public-api.wordpress.com/wp/v2/sites/{id}`) in [SiteModel.getWpApiRestUrl] — the
     * same value simple `.wordpress.com` sites get. That URL already contains `/wp/v2/`, so letting
     * wordpress-rs append the standard `/wp/v2/...` route onto it produces a doubled `/wp/v2/` and a
     * 404 (`rest_no_route`). These sites reach their `wp-admin` directly, so derive the root from the
     * site host instead of trusting the stored proxy URL. Genuine self-hosted sites keep their
     * discovered [SiteModel.getWpApiRestUrl].
     *
     * The proxy URL is detected two ways to stay robust: the [SiteModel.isUsingWpComRestApi] flag,
     * and — for migrated sites that carry the proxy URL without being flagged — the `/wp/v2/` route
     * segment in the stored value itself. Either match falls back to the direct host root.
     */
    private fun resolveApiRoot(site: SiteModel): String {
        val stored = site.wpApiRestUrl?.takeIf { it.isNotEmpty() }
        if (site.isUsingWpComRestApi || stored == null || isWpComProxyApiRestUrl(stored)) {
            return "${site.url}/wp-json"
        }
        return stored
    }

    // The WP.com REST proxy root looks like https://public-api.wordpress.com/wp/v2/sites/{id}; a
    // genuine self-hosted wp-json root never contains the /wp/v2/ route segment.
    private fun isWpComProxyApiRestUrl(apiRestUrl: String): Boolean = apiRestUrl.contains("/wp/v2/")

    private fun createDelegate(site: SiteModel): WpApiClientDelegate {
        val authProvider = if (site.isWPCom) {
            createWpComAuthProvider(accountStore)
        } else {
            val username = site.apiRestUsernamePlain
            val password = site.apiRestPasswordPlain
            require(!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                "Application password credentials missing for site"
            }
            WpAuthenticationProvider.staticWithUsernameAndPassword(username, password)
        }

        return WpApiClientDelegate(
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(
                interceptors = emptyList(),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                    wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                }
            }
        )
    }

    @Synchronized
    private fun getOrCreateCache(): WordPressApiCache {
        return cache ?: run {
            val cacheDir = File(context.filesDir, "wp_rs_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val dbPath = File(cacheDir, "wp_api_cache.db").absolutePath
            val newCache = WordPressApiCache(dbPath)
            newCache.performMigrations()
            newCache.cache.startListeningForUpdates(DatabaseChangeNotifier)
            cache = newCache
            newCache
        }
    }
}
