package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
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
import javax.inject.Inject

class JetpackConnectionHelper @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper
) {
    fun initWpApiClient(site: SiteModel): WpApiClient {
        requireRestCredentials(site)
        return wpApiClientProvider.getWpApiClient(site)
    }

    fun initJetpackConnectionClient(site: SiteModel): JetpackConnectionClient {
        requireRestCredentials(site)

        val delegate = WpApiClientDelegate(
            authProvider = createRestAuthProvider(site),
            requestExecutor = WpRequestExecutor(interceptors = emptyList()),
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            appNotifier = InvalidAuthNotifier()
        )

        return JetpackConnectionClient(
            apiRootUrl = ParsedUrl.parse(resolveRestApiUrl(site)),
            delegate = delegate
        )
    }

    private fun createRestAuthProvider(site: SiteModel) =
        WpAuthenticationProvider.staticWithUsernameAndPassword(
            site.apiRestUsernamePlain!!,
            site.apiRestPasswordPlain!!
        )

    private fun requireRestCredentials(site: SiteModel) {
        require(!site.apiRestUsernamePlain.isNullOrBlank()) {
            "API username is required"
        }
        require(!site.apiRestPasswordPlain.isNullOrBlank()) {
            "API password is required"
        }
    }

    private fun resolveRestApiUrl(site: SiteModel) =
        site.wpApiRestUrl ?: "${site.url}/wp-json"

    private inner class InvalidAuthNotifier : WpAppNotifier {
        override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
            appLogWrapper.d(AppLog.T.API, "$TAG: requestedWithInvalidAuthentication")
            throw IllegalArgumentException("Invalid credentials")
        }
    }

    companion object {
        private const val TAG = "JetpackConnectionHelper"
    }
}
