package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRsRestClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) {
    suspend fun trashPost(site: SiteModel, postId: Long): PostActionResult {
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request { it.posts().trash(PostEndpointType.Posts, postId) }
        return when (response) {
            is WpRequestResult.Success -> PostActionResult.Success
            else -> PostActionResult.Error(parseErrorMessage(response))
        }
    }

    suspend fun deletePost(site: SiteModel, postId: Long): PostActionResult {
        val client = wpApiClientProvider.getWpApiClient(site)
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
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.posts().update(PostEndpointType.Posts, postId, PostUpdateParams(status = newStatus, meta = null))
        }
        return when (response) {
            is WpRequestResult.Success -> PostActionResult.Success
            else -> PostActionResult.Error(parseErrorMessage(response))
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
