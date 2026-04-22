package org.wordpress.android.networking.restapi

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.rs.RsSite
import org.wordpress.android.networking.rs.WpApiClientProvider
import org.wordpress.android.networking.rs.shouldUseWpComProxy
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.ApiUrlResolver
import uniffi.wp_api.CookiesNonceAuthenticationProvider
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpComBaseUrl
import uniffi.wp_api.WpComDotOrgApiUrlResolver as WpComUrlResolver // checkstyle ignore
import uniffi.wp_api.WpOrgSiteApiUrlResolver
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WpApiClientProviderImpl @Inject constructor(
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    private val accountStore: AccountStore,
    @Named(OkHttpClientQualifiers.INTERCEPTORS) private val interceptors: Set<@JvmSuppressWildcards Interceptor>,
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
) : WpApiClientProvider {
    private val wpComClients = mutableMapOf<Long, WpApiClient>()
    private val selfHostedClients = mutableMapOf<Int, WpApiClient>()

    @Synchronized
    override fun clearAllClients() {
        wpComClients.clear()
        selfHostedClients.clear()
    }

    @Synchronized
    override fun clearSelfHostedClient(siteId: Int) {
        selfHostedClients.remove(siteId)
    }

    @Synchronized
    override fun getWpApiClient(
        site: RsSite,
        uploadListener: WpRequestExecutor.UploadListener?
    ): WpApiClient = when {
        site.shouldUseWpComProxy() -> getWpComApiClient(site)
        // Skip caching when an upload listener is provided —
        // upload flows need a dedicated client with progress
        // callbacks.
        uploadListener != null ->
            createSelfHostedClient(site, uploadListener)
        else -> selfHostedClients.getOrPut(site.localId) {
            createSelfHostedClient(site, uploadListener = null)
        }
    }

    private fun createSelfHostedClient(
        site: RsSite,
        uploadListener: WpRequestExecutor.UploadListener?,
    ): WpApiClient {
        val authProvider =
            WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = site.apiRestUsernamePlain,
                password = site.apiRestPasswordPlain,
            )

        val urlResolver =
            WpOrgSiteApiUrlResolver(ParsedUrl.parse(site.buildRestApiUrl()))

        return WpApiClient(
            apiUrlResolver = urlResolver,
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(
                interceptors = interceptors.toList(),
                networkAvailabilityProvider =
                    networkAvailabilityProvider,
                uploadListener = uploadListener,
            ),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(
                    requestUrl: String
                ) {
                    wpAppNotifierHandler
                        .notifyRequestedWithInvalidAuthentication(
                            site.url
                        )
                }
            },
        )
    }

    override fun getWpApiClientCookiesNonceAuthentication(site: RsSite): WpApiClient {
        // Create OkHttpClient with cookie jar for cookies/nonce authentication
        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(object : CookieJar {
                // We are storing the cookie in memory as this is a one-time call and there is no need to persist it
                private val cookieStore = mutableMapOf<String, List<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: emptyList()
                }
            })
            .apply { interceptors.forEach { addInterceptor(it) } }
            .build()

        val httpClient = WpHttpClient.CustomOkHttpClient(okHttpClient)
        val requestExecutor = WpRequestExecutor(httpClient, networkAvailabilityProvider)

        val cookiesNonceProvider = CookiesNonceAuthenticationProvider.withSiteUrl(
            url = site.url,
            username = site.username,
            password = site.password,
            requestExecutor = requestExecutor
        )
        val authProvider = WpAuthenticationProvider.dynamic(cookiesNonceProvider)
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.buildRestApiUrl()),
            authProvider = authProvider,
            requestExecutor = requestExecutor,
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                    wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site.url)
                }
            }
        )
        return client
    }

    @Synchronized
    private fun getWpComApiClient(site: RsSite): WpApiClient {
        return wpComClients.getOrPut(site.siteId) {
            val urlResolver = WpComUrlResolver(
                siteId = site.siteId.toString(),
                baseUrl = WpComBaseUrl.Production
            )
            WpApiClient(
                apiUrlResolver = urlResolver,
                authProvider = createWpComAuthProvider(accountStore),
                requestExecutor = WpRequestExecutor(emptyList(), networkAvailabilityProvider),
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site.url)
                    }
                }
            )
        }
    }

    override fun getApiUrlResolver(site: RsSite): ApiUrlResolver =
        if (site.shouldUseWpComProxy()) {
            WpComUrlResolver(
                siteId = site.siteId.toString(),
                baseUrl = WpComBaseUrl.Production
            )
        } else {
            WpOrgSiteApiUrlResolver(ParsedUrl.parse(site.buildRestApiUrl()))
        }

    override fun getApiRootUrlFrom(site: RsSite): String = site.buildRestApiUrl()
}
