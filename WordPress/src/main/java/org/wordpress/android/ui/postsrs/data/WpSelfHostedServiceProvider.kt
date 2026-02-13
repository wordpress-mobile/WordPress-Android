package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.cache.kotlin.DatabaseChangeNotifier
import rs.wordpress.cache.kotlin.WordPressApiCache
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpOrgSiteApiUrlResolver
import uniffi.wp_mobile.WpSelfHostedService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WpSelfHostedServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpAppNotifierHandler: WpAppNotifierHandler,
) {
    private val services = mutableMapOf<Long, WpSelfHostedService>()
    private var cache: WordPressApiCache? = null

    @Synchronized
    fun getService(site: SiteModel): WpSelfHostedService {
        return services.getOrPut(site.siteId) {
            createService(site)
        }
    }

    /**
     * Builds a [WpSelfHostedService] for the given site using its
     * application-password credentials and REST API root URL.
     */
    private fun createService(site: SiteModel): WpSelfHostedService {
        val apiRoot = site.wpApiRestUrl
            ?.takeIf { it.isNotEmpty() }
            ?: "${site.url}/wp-json"

        val username = site.apiRestUsernamePlain
        val password = site.apiRestPasswordPlain
        require(!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            "Application password credentials missing for site"
        }

        val apiUrlResolver = WpOrgSiteApiUrlResolver(
            ParsedUrl.parse(apiRoot)
        )

        val authProvider =
            WpAuthenticationProvider.staticWithUsernameAndPassword(
                username,
                password
            )

        val delegate = WpApiClientDelegate(
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(emptyList()),
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(
                    requestUrl: String
                ) {
                    wpAppNotifierHandler
                        .notifyRequestedWithInvalidAuthentication(site)
                }
            }
        )

        val wpApiCache = getOrCreateCache()

        return WpSelfHostedService(
            site.url,
            apiRoot,
            apiUrlResolver,
            delegate,
            wpApiCache.cache
        )
    }

    /**
     * Returns the shared [WordPressApiCache], creating and initializing
     * the backing SQLite database on first access.
     */
    private fun getOrCreateCache(): WordPressApiCache {
        return cache ?: run {
            val cacheDir = File(context.filesDir, "wp_rs_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val dbPath = File(cacheDir, "wp_api_cache.db").absolutePath
            val newCache = WordPressApiCache(dbPath)
            newCache.performMigrations()
            newCache.cache.startListeningForUpdates(
                DatabaseChangeNotifier
            )
            cache = newCache
            newCache
        }
    }
}
