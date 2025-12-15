package org.wordpress.android.modules

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.posttypes.bridge.WpSelfHostedServiceFactory
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.cache.kotlin.WordPressApiCache
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpOrgSiteApiUrlResolver
import uniffi.wp_mobile.WpSelfHostedService
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val CACHE_FILE_NAME = "wordpress_api_cache.db"

@Module
@InstallIn(SingletonComponent::class)
abstract class WordPressRsModule {
    @Binds
    abstract fun bindWpSelfHostedServiceFactory(
        impl: WpSelfHostedServiceFactoryImpl
    ): WpSelfHostedServiceFactory

    companion object {
        @Provides
        @Singleton
        fun provideWordPressApiCache(
            @ApplicationContext context: Context
        ): WordPressApiCache {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            return WordPressApiCache(cacheFile.toPath()).apply {
                performMigrations()
            }
        }
    }
}

/**
 * Implementation of [WpSelfHostedServiceFactory] that creates [WpSelfHostedService] instances
 * using the same configuration pattern as [WpApiClientProvider].
 *
 * This ensures consistency with how [WpApiClient] is configured:
 * - Bearer token authentication for WordPress.com sites
 * - Application password authentication for self-hosted sites
 * - Same request executor with interceptors
 * - Same app notifier pattern
 */
@Singleton
class WpSelfHostedServiceFactoryImpl @Inject constructor(
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    @Named(OkHttpClientQualifiers.INTERCEPTORS)
    private val interceptors: Set<@JvmSuppressWildcards Interceptor>,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val cache: WordPressApiCache
) : WpSelfHostedServiceFactory {

    override fun create(siteId: Long, siteUrl: String): WpSelfHostedService {
        // siteId is the remote WordPress.com site ID, not the local database ID
        val site = siteStore.getSiteBySiteId(siteId)
            ?: throw IllegalArgumentException("Site not found for remote site id: $siteId")

        val apiRoot = site.wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "$siteUrl/wp-json"
        val authProvider = createAuthProvider(site)

        val delegate = WpApiClientDelegate(
            authProvider,
            requestExecutor = WpRequestExecutor(interceptors.toList()),
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            appNotifier = createAppNotifier(site)
        )

        return WpSelfHostedService(
            siteUrl = siteUrl,
            apiRoot = apiRoot,
            apiUrlResolver = WpOrgSiteApiUrlResolver(ParsedUrl.parse(apiRoot)),
            delegate = delegate,
            cache = cache.cache
        )
    }

    private fun createAuthProvider(site: SiteModel): WpAuthenticationProvider {
        // WordPress.com sites use bearer token authentication
        if (site.isWPCom || site.isUsingWpComRestApi) {
            val accessToken = accountStore.accessToken
            require(!accessToken.isNullOrEmpty()) {
                "Access token is required for WordPress.com site: ${site.name}"
            }
            return WpAuthenticationProvider.staticWithAuth(
                WpAuthentication.Bearer(token = accessToken)
            )
        }

        // Self-hosted sites use application password authentication
        val username = site.apiRestUsernamePlain
        val password = site.apiRestPasswordPlain

        require(!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            "Site does not have application password credentials. " +
                "Please set up application passwords for site: ${site.name}"
        }

        return WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = username,
            password = password
        )
    }

    private fun createAppNotifier(site: SiteModel): WpAppNotifier {
        return object : WpAppNotifier {
            override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
            }
        }
    }
}
