package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
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
    private val mediaUrlCache = mutableMapOf<Long, String>()

    suspend fun fetchMediaUrl(site: SiteModel, mediaId: Long): String? {
        mediaUrlCache[mediaId]?.let { return it }
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.media().retrieveWithEditContext(mediaId)
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val sourceUrl = response.response.data.sourceUrl
                val url = toPhotonUrl(site, sourceUrl)
                mediaUrlCache[mediaId] = url
                url
            }
            else -> {
                AppLog.w(AppLog.T.POSTS, "fetchMediaUrl: mediaId=$mediaId failed: $response")
                null
            }
        }
    }

    private fun toPhotonUrl(site: SiteModel, sourceUrl: String): String {
        if (!SiteUtils.isPhotonCapable(site)) return sourceUrl
        val density = context.resources.displayMetrics.density
        val sizePx = (FEATURED_IMAGE_SIZE_DP * density).toInt()
        return PhotonUtils.getPhotonImageUrl(
            sourceUrl, sizePx, sizePx, site.isPrivateWPComAtomic
        )
    }

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

    companion object {
        private const val FEATURED_IMAGE_SIZE_DP = 64
    }
}
