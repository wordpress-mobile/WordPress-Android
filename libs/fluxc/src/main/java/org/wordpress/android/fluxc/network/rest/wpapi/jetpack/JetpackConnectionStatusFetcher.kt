package org.wordpress.android.fluxc.network.rest.wpapi.jetpack

import okhttp3.Interceptor
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.EmptyAppNotifier
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.JetpackConnectionClient
import uniffi.wp_api.JetpackConnectionStatus
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiClientDelegate
import uniffi.wp_api.WpApiMiddlewarePipeline
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * A self-hosted site's Jetpack connection to WordPress.com. [wpComSiteId] is the blog ID WordPress.com
 * assigned the site when it connected, and is null whenever [isConnected] is false.
 */
data class JetpackConnectionState(
    val isConnected: Boolean,
    val wpComSiteId: Long?
)

/**
 * Reads a self-hosted site's Jetpack connection state over wordpress-rs.
 *
 * Only the Jetpack plugin knows whether a site is connected to WordPress.com and which blog ID it was
 * given. Sites added with an application password have no other source for either: the WP.com REST site
 * payload that populates those fields for Jetpack sites is never fetched for them.
 */
@Singleton
class JetpackConnectionStatusFetcher @Inject constructor(
    @Named(OkHttpClientQualifiers.INTERCEPTORS) interceptors: Set<@JvmSuppressWildcards Interceptor>,
    networkAvailabilityProvider: WpNetworkAvailabilityProvider,
    private val appLogWrapper: AppLogWrapper
) {
    // One executor for the singleton: each WpRequestExecutor builds its own OkHttpClient (with its own
    // connection pool), and the executor is site-independent -- auth lives in the per-call delegate.
    private val requestExecutor = WpRequestExecutor(
        interceptors = interceptors.toList(),
        networkAvailabilityProvider = networkAvailabilityProvider
    )

    /**
     * Returns the site's Jetpack connection state, or null when it can't be determined — the site is
     * unreachable, the endpoint isn't there, or Jetpack is older than 14.2, which is where these
     * endpoints were added. Callers should read null as "unchanged", not as "not connected".
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetch(
        apiRootUrl: String,
        username: String,
        password: String
    ): JetpackConnectionState? = try {
        buildClient(apiRootUrl, username, password).use { client ->
            when (val status = client.status()) {
                is JetpackConnectionStatus.NotConnected -> JetpackConnectionState(
                    isConnected = false,
                    wpComSiteId = null
                )

                is JetpackConnectionStatus.Site -> JetpackConnectionState(
                    isConnected = true,
                    wpComSiteId = status.blogId.toLong()
                )

                is JetpackConnectionStatus.User -> JetpackConnectionState(
                    isConnected = true,
                    wpComSiteId = status.blogId.toLong()
                )
            }
        }
    } catch (e: Exception) {
        appLogWrapper.d(AppLog.T.API, "$TAG: couldn't read the Jetpack connection status: ${e.message}")
        null
    }

    private fun buildClient(
        apiRootUrl: String,
        username: String,
        password: String
    ) = JetpackConnectionClient(
        apiRootUrl = ParsedUrl.parse(apiRootUrl),
        delegate = WpApiClientDelegate(
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(username, password),
            requestExecutor = requestExecutor,
            middlewarePipeline = WpApiMiddlewarePipeline(emptyList()),
            // Reading the status is a passive check made during a site refresh, so a rejected
            // credential shouldn't raise the app-wide "re-authenticate" prompt on its own.
            appNotifier = EmptyAppNotifier()
        )
    )

    companion object {
        private const val TAG = "JetpackConnectionStatusFetcher"
    }
}
