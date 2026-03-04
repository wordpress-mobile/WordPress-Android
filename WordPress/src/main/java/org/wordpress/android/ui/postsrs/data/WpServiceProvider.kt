package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.createWpComAuthProvider
import org.wordpress.android.fluxc.store.AccountStore
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.cache.kotlin.DatabaseChangeNotifier
import rs.wordpress.cache.kotlin.WordPressApiCache
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
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
        val delegate = createDelegate(site)
        val wpApiCache = getOrCreateCache()
        return if (site.isWPCom) {
            WpService.wordpressCom(site.siteId.toULong(), delegate, wpApiCache.cache)
        } else {
            val apiRoot = site.wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "${site.url}/wp-json"
            WpService.selfHosted(site.url, apiRoot, delegate, wpApiCache.cache)
        }
    }

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
            requestExecutor = WpRequestExecutor(emptyList()),
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
