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
    private val services = mutableMapOf<Int, CachedService>()
    private var cache: WordPressApiCache? = null

    @Synchronized
    fun getService(site: SiteModel): WpService {
        val key = ServiceKey.from(site)
        services[site.id]?.takeIf { it.key == key }?.let { return it.service }
        return CachedService(key, createService(site))
            .also { services[site.id] = it }
            .service
    }

    /** Removes all cached services. */
    @Synchronized
    fun clearAll() {
        services.clear()
    }

    private fun createService(site: SiteModel): WpService {
        // Same transport rule as WpApiClientProvider.getWpApiClient, so the list and the
        // edit-time fetch that follows it can't disagree about the transport.
        val useWpCom = site.isUsingWpComRestApi
        val delegate = createDelegate(site)
        val wpApiCache = getOrCreateCache()
        val siteInfo = if (useWpCom) {
            SiteInfo.WordPressCom(siteId = site.siteId.toULong())
        } else {
            val apiRoot = site.wpApiRestUrl?.takeIf { it.isNotEmpty() }
                ?: "${site.url}/wp-json"
            SiteInfo.SelfHosted(
                siteUrl = ParsedUrl.parse(site.url),
                apiRoot = ParsedUrl.parse(apiRoot),
            )
        }
        return WpService(siteInfo, delegate, wpApiCache.cache)
    }

    private fun createDelegate(site: SiteModel): WpApiClientDelegate {
        val authProvider = if (site.isUsingWpComRestApi) {
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
            appNotifier = createAppNotifier(site)
        )
    }

    /**
     * [WpAppNotifierHandler] listeners respond by launching application-password
     * reauthentication, which can only fix a Basic-auth failure. A 401 on the WP.com bearer
     * transport means an expired or revoked OAuth token — the rs screens surface that error
     * themselves — so bearer-authenticated services don't report to the handler at all.
     */
    private fun createAppNotifier(site: SiteModel): WpAppNotifier = if (site.isUsingWpComRestApi) {
        object : WpAppNotifier {
            override suspend fun requestedWithInvalidAuthentication(requestUrl: String) = Unit
        }
    } else {
        object : WpAppNotifier {
            override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
            }
        }
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

    private class CachedService(val key: ServiceKey, val service: WpService)

    /**
     * The inputs a cached [WpService] was built from. An entry is only reused while these still
     * match the site, so a service built with a revoked application password (reauthentication
     * stores new credentials) or on the wrong transport (a site refresh can rewrite
     * [SiteModel.getOrigin]) doesn't outlive the state it was built from. The WP.com bearer token
     * is read per request (see [createWpComAuthProvider]) so it doesn't participate.
     */
    private data class ServiceKey(
        val useWpCom: Boolean,
        val username: String?,
        val password: String?,
    ) {
        companion object {
            fun from(site: SiteModel): ServiceKey {
                val useWpCom = site.isUsingWpComRestApi
                return ServiceKey(
                    useWpCom = useWpCom,
                    username = if (useWpCom) null else site.apiRestUsernamePlain,
                    password = if (useWpCom) null else site.apiRestPasswordPlain,
                )
            }
        }
    }
}
