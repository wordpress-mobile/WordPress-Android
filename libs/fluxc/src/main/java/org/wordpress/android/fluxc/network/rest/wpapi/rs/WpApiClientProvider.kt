package org.wordpress.android.fluxc.network.rest.wpapi.rs

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.AccountStore
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.CookiesNonceAuthenticationProvider
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpComBaseUrl
import uniffi.wp_api.WpComDotOrgApiUrlResolver as WpComUrlResolver // checkstyle ignore
import uniffi.wp_api.WpDynamicAuthenticationProvider
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WpApiClientProvider @Inject constructor(
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    private val accountStore: AccountStore,
    @Named(OkHttpClientQualifiers.INTERCEPTORS) private val interceptors: Set<@JvmSuppressWildcards Interceptor>,
) {
    private val wpComClients = mutableMapOf<Long, WpApiClient>()

    /** Removes all cached WP.com API clients (e.g. on sign-out). */
    @Synchronized
    fun clearWpComClients() {
        wpComClients.clear()
    }

    fun getWpApiClient(
        site: SiteModel,
        uploadListener: WpRequestExecutor.UploadListener? = null
    ): WpApiClient {
        if (site.isWPCom) return getWpComApiClient(site)
        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
        )
        val apiRootUrl = URL(site.buildUrl())
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = apiRootUrl,
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(interceptors = interceptors.toList(), uploadListener = uploadListener),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                    wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                }
            }
        )
        return client
    }

    fun getWpApiClientCookiesNonceAuthentication(site: SiteModel): WpApiClient {
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
        val requestExecutor = WpRequestExecutor(httpClient)

        val cookiesNonceProvider = CookiesNonceAuthenticationProvider.withSiteUrl(
            url = site.url,
            username = site.username,
            password = site.password,
            requestExecutor = requestExecutor
        )
        val authProvider = WpAuthenticationProvider.dynamic(cookiesNonceProvider)
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.buildUrl()),
            authProvider = authProvider,
            requestExecutor = requestExecutor,
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                    wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                }
            }
        )
        return client
    }

    @Synchronized
    private fun getWpComApiClient(site: SiteModel): WpApiClient {
        return wpComClients.getOrPut(site.siteId) {
            val urlResolver = WpComUrlResolver(
                siteId = site.siteId.toString(),
                baseUrl = WpComBaseUrl.Production
            )
            WpApiClient(
                apiUrlResolver = urlResolver,
                authProvider = createWpComAuthProvider(accountStore),
                requestExecutor = WpRequestExecutor(emptyList()),
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                    }
                }
            )
        }
    }

    fun getApiRootUrlFrom(site: SiteModel): String = site.buildUrl()

    private fun SiteModel.buildUrl(): String =
        wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "${url}/wp-json"
}

/**
 * Creates a [WpAuthenticationProvider] that reads the WordPress.com OAuth bearer token from
 * [AccountStore] on every request. This ensures cached API clients automatically pick up
 * refreshed tokens without needing to be recreated.
 */
fun createWpComAuthProvider(accountStore: AccountStore): WpAuthenticationProvider =
    WpAuthenticationProvider.dynamic(object : WpDynamicAuthenticationProvider {
        override fun auth() = WpAuthentication.Bearer(
            token = requireNotNull(accountStore.accessToken) {
                "WP.com access token is required"
            }
        )
        override suspend fun refresh() = accountStore.accessToken != null
    })
