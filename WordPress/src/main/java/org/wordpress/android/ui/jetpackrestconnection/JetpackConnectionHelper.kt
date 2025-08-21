package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VersionUtils.checkMinimalVersion
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
            requestExecutor = WpRequestExecutor(),
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
        override suspend fun requestedWithInvalidAuthentication() {
            appLogWrapper.d(AppLog.T.API, "$TAG: requestedWithInvalidAuthentication")
            throw IllegalArgumentException("Invalid credentials")
        }
    }

    /**
     * Returns true if the Jetpack REST Connection flow is available and this site is able to use it.
     * Requirements:
     * - Jetpack app
     * - Self-hosted site authenticated with application password,
     * - Site isn't already connected to Jetpack
     * - Jetpack is not installed or the installed jetpack version is 14.2 or above
     */
    fun canInitiateJetpackRestConnection(site: SiteModel): Boolean {
        return BuildConfig.IS_JETPACK_APP
                && site.isUsingSelfHostedRestApi
                && !site.wpApiRestUrl.isNullOrEmpty()
                && !site.isJetpackConnected
                && (!site.isJetpackInstalled || checkMinimalVersion(site.jetpackVersion, JETPACK_LIMIT_VERSION))
    }

    companion object {
        private const val TAG = "JetpackConnectionHelper"
        private const val JETPACK_LIMIT_VERSION = "14.2"
    }
}
