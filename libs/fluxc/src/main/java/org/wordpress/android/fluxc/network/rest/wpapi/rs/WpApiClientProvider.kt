package org.wordpress.android.fluxc.network.rest.wpapi.rs

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.CookiesNonceAuthenticationProvider
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

class WpApiClientProvider @Inject constructor(
    private val wpAppNotifierHandler: WpAppNotifierHandler,
) {
    fun getWpApiClient(
        site: SiteModel,
        uploadListener: WpRequestExecutor.UploadListener? = null
    ): WpApiClient {
        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
        )
        val apiRootUrl = URL(site.buildUrl())
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = apiRootUrl,
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(uploadListener = uploadListener),
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

    fun getApiRootUrlFrom(site: SiteModel): String = site.buildUrl()

    private fun SiteModel.buildUrl(): String =
        wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "${url}/wp-json"
}
