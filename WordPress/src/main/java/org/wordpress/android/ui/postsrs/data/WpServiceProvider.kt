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

    private fun createService(site: SiteModel): WpService {
        val useWpCom = site.useWpCom()
        val delegate = createDelegate(site, useWpCom)
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

    /**
     * Whether the site is served over the WP.com REST API with an OAuth bearer token rather than
     * directly with an application password. Deliberately the same rule as
     * [org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider.getWpApiClient] so the
     * list and the edit-time fetch that follows it can't disagree about the transport.
     */
    private fun SiteModel.useWpCom(): Boolean = isWPCom || isUsingWpComRestApi

    private fun createDelegate(site: SiteModel, useWpCom: Boolean): WpApiClientDelegate {
        val authProvider = if (useWpCom) {
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
