package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
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
import uniffi.wp_api.WpOrgSiteApiUrlResolver
import javax.inject.Inject

class JetpackConnectionHelper @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    private val trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor,
    private val networkAvailabilityProvider: WpNetworkAvailabilityProvider,
) {
    /**
     * Builds a **dedicated** [WpApiClient] for the Jetpack plugin-install step, talking to the site's
     * own REST host with its application-password credentials.
     *
     * Unlike [org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider.getWpApiClient], a
     * 401 from this client is handled **locally** via [onInvalidAuth] rather than broadcast on the
     * app-wide `WpAppNotifierHandler`. An invalid-auth here is the connection flow's own concern (the
     * view model resets and restarts it); broadcasting it would also wake the app-wide
     * SiteProvisioningSource, whose concurrent validate / wipe / re-mint of these same credentials can
     * race this in-flight connection — e.g. clearing the `apiRest*` columns that [requireRestCredentials]
     * needs, mid-install. Keeping the install client off that bus leaves SiteProvisioningSource as the
     * single global invalid-auth authority. This mirrors [initJetpackConnectionClient], whose
     * [InvalidAuthNotifier] is likewise connection-local.
     *
     * [onInvalidAuth] fires at most once per client: install issues up to two requests (list, then
     * create) and the flow should only restart once.
     */
    fun initWpApiClient(site: SiteModel, onInvalidAuth: () -> Unit): WpApiClient {
        requireRestCredentials(site)
        return WpApiClient(
            apiUrlResolver = WpOrgSiteApiUrlResolver(ParsedUrl.parse(resolveRestApiUrl(site))),
            authProvider = createRestAuthProvider(site),
            requestExecutor = WpRequestExecutor(
                interceptors = listOf(trackNetworkRequestsInterceptor),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
            appNotifier = object : WpAppNotifier {
                private var handled = false
                override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                    if (handled) return
                    handled = true
                    appLogWrapper.d(AppLog.T.API, "$TAG: requestedWithInvalidAuthentication (install client)")
                    onInvalidAuth()
                }
            },
        )
    }

    fun initJetpackConnectionClient(site: SiteModel): JetpackConnectionClient {
        requireRestCredentials(site)

        val delegate = WpApiClientDelegate(
            authProvider = createRestAuthProvider(site),
            requestExecutor = WpRequestExecutor(
                interceptors = listOf(trackNetworkRequestsInterceptor),
                networkAvailabilityProvider = networkAvailabilityProvider
            ),
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

    // takeIf: wpApiRestUrl can be an empty string as well as null (the column is cleared, not
    // dropped), and ParsedUrl.parse("") throws. Mirrors WpApiClientProvider.buildUrl, which the
    // install client used before it got its own notifier-free client.
    private fun resolveRestApiUrl(site: SiteModel) =
        site.wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "${site.url}/wp-json"

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
