package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.JetpackConnectionClient
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

class JetpackConnectionHelper @Inject constructor(
    private val appLogWrapper: AppLogWrapper
) {
    fun initWpApiClient(site: SiteModel): WpApiClient {
        requireCredentials(site)
        return WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = createAuthProvider(site)
        )
    }

    fun initJetpackConnectionClient(site: SiteModel): JetpackConnectionClient {
        requireCredentials(site)
        
        val delegate = WpApiClientDelegate(
            authProvider = createAuthProvider(site),
            requestExecutor = WpRequestExecutor(),
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            appNotifier = InvalidAuthNotifier()
        )

        return JetpackConnectionClient(
            apiRootUrl = ParsedUrl.parse(resolveApiUrl(site)),
            delegate = delegate
        )
    }

    private fun createAuthProvider(site: SiteModel) = 
        WpAuthenticationProvider.staticWithUsernameAndPassword(
            site.apiRestUsernamePlain!!,
            site.apiRestPasswordPlain!!
        )

    private fun requireCredentials(site: SiteModel) {
        require(!site.apiRestUsernamePlain.isNullOrBlank()) { 
            "API username is required" 
        }
        require(!site.apiRestPasswordPlain.isNullOrBlank()) { 
            "API password is required" 
        }
    }

    private fun resolveApiUrl(site: SiteModel) = 
        site.wpApiRestUrl ?: "${site.url}/wp-json"

    private inner class InvalidAuthNotifier : WpAppNotifier {
        override suspend fun requestedWithInvalidAuthentication() {
            appLogWrapper.d(AppLog.T.API, "$TAG: requestedWithInvalidAuthentication")
            throw IllegalArgumentException("Invalid credentials")
        }
    }

    companion object {
        const val CONNECT_FROM = "jetpack-android-app"
        private const val TAG = "JetpackConnectionHelper"
    }
}
