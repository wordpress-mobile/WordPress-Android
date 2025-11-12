package org.wordpress.android.fluxc.network.rest.wpapi.rs

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.CookiesNonceAuthenticationProvider
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiDetails
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
        val apiRoot = site.buildUrl()
        val apiRootUrl = URL(apiRoot)
        val requestExecutor = WpRequestExecutor()
        val cookiesNonceProvider = CookiesNonceAuthenticationProvider.withSiteUrl(
            url = apiRoot,
            username = site.username,
            password = site.password,
            requestExecutor = requestExecutor
        )
        val authProvider = WpAuthenticationProvider.dynamic(cookiesNonceProvider)
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = apiRootUrl,
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

    private fun SiteModel.buildUrl(): String = wpApiRestUrl ?: "${url}/wp-json"
}
