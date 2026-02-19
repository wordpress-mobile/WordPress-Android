package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpComBaseUrl
import uniffi.wp_api.WpComDotOrgApiUrlResolver as WpComUrlResolver // checkstyle ignore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRsRestClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val wpAppNotifierHandler: WpAppNotifierHandler,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) {
    private val wpComClients = mutableMapOf<Long, WpApiClient>()

    /** Removes all cached WP.com API clients (e.g. on sign-out). */
    @Synchronized
    fun clearWpComClients() {
        wpComClients.clear()
    }

    suspend fun trashPost(site: SiteModel, postId: Long): PostActionResult {
        val client = getApiClient(site)
        val response = client.request { it.posts().trash(PostEndpointType.Posts, postId) }
        return when (response) {
            is WpRequestResult.Success -> PostActionResult.Success
            else -> PostActionResult.Error(parseErrorMessage(response))
        }
    }

    suspend fun deletePost(site: SiteModel, postId: Long): PostActionResult {
        val client = getApiClient(site)
        val response = client.request { it.posts().delete(PostEndpointType.Posts, postId) }
        return when (response) {
            is WpRequestResult.Success -> {
                if (response.response.data.deleted) {
                    PostActionResult.Success
                } else {
                    PostActionResult.Error(context.getString(R.string.post_rs_error_delete))
                }
            }
            else -> PostActionResult.Error(parseErrorMessage(response))
        }
    }

    suspend fun updatePostStatus(site: SiteModel, postId: Long, newStatus: PostStatus): PostActionResult {
        val client = getApiClient(site)
        val response = client.request {
            it.posts().update(PostEndpointType.Posts, postId, PostUpdateParams(status = newStatus, meta = null))
        }
        return when (response) {
            is WpRequestResult.Success -> PostActionResult.Success
            else -> PostActionResult.Error(parseErrorMessage(response))
        }
    }

    /**
     * Returns an API client for [site]. Self-hosted sites delegate to [WpApiClientProvider];
     * WordPress.com sites use a cached [WpApiClient] with bearer-token auth and a
     * WP.com-specific URL resolver for correct URL construction.
     */
    @Synchronized
    private fun getApiClient(site: SiteModel): WpApiClient {
        if (!site.isWPCom) return wpApiClientProvider.getWpApiClient(site)
        return wpComClients.getOrPut(site.siteId) {
            val urlResolver = WpComUrlResolver(
                siteId = site.siteId.toString(),
                baseUrl = WpComBaseUrl.Production
            )
            val authProvider = createWpComAuthProvider(accountStore)
            WpApiClient(
                apiUrlResolver = urlResolver,
                authProvider = authProvider,
                requestExecutor = WpRequestExecutor(emptyList()),
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication(requestUrl: String) {
                        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                    }
                }
            )
        }
    }

    private fun parseErrorMessage(response: WpRequestResult<*>): String {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return context.getString(R.string.no_network_message)
        }
        return when (response) {
            is WpRequestResult.WpError<*> ->
                response.errorMessage.takeIf { it.isNotBlank() } ?: context.getString(R.string.request_failed_message)
            else -> context.getString(R.string.request_failed_message)
        }
    }

    sealed class PostActionResult {
        data object Success : PostActionResult()
        data class Error(val message: String) : PostActionResult()
    }
}
