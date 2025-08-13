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
        if (!validateCredentials(site)) {
            throw IllegalArgumentException("Missing credentials for WpApiClient")
        }
        return WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                site.apiRestUsernamePlain!!,
                site.apiRestPasswordPlain!!
            )
        )
    }

    fun initJetpackConnectionClient(site: SiteModel): JetpackConnectionClient {
        if (!validateCredentials(site)) {
            throw IllegalArgumentException("Missing credentials for JetpackConnectionClient")
        }

        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            site.apiRestUsernamePlain!!,
            site.apiRestPasswordPlain!!
        )

        val delegate = WpApiClientDelegate(
            authProvider = authProvider,
            requestExecutor = WpRequestExecutor(),
            middlewarePipeline = WpApiMiddlewarePipeline(
                emptyList()
            ),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication() {
                    appLogWrapper.d(AppLog.T.API, "$TAG: requestedWithInvalidAuthentication")
                    throw IllegalArgumentException("Invalid credentials for JetpackConnectionClient")
                }
            }
        )

        val apiUrl = site.wpApiRestUrl ?: "${site.url}/wp-json"
        return JetpackConnectionClient(
            apiRootUrl = ParsedUrl.parse(apiUrl),
            delegate = delegate
        )
    }

    private fun validateCredentials(site: SiteModel): Boolean {
        return !site.apiRestUsernamePlain.isNullOrBlank() && !site.apiRestPasswordPlain.isNullOrBlank()
    }

    companion object {
        const val CONNECT_FROM = "jetpack-android-app"
        private const val TAG = "JetpackConnectionHelper"
    }
}
