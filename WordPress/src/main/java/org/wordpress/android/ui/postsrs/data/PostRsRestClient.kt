package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.UserListParams
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRsRestClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) {
    private val mediaUrlCache = ConcurrentHashMap<Long, String>()
    private val userNameCache = ConcurrentHashMap<Long, String>()

    fun clearCaches() {
        mediaUrlCache.clear()
        userNameCache.clear()
    }

    /**
     * Fetches media source URLs for the given [mediaIds] in a single
     * network call using the `include` parameter, returning a map of
     * media ID to Photon-optimised URL. IDs already in the local cache
     * are returned immediately without a network round-trip.
     */
    suspend fun fetchMediaUrls(
        site: SiteModel,
        mediaIds: List<Long>
    ): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in mediaIds) {
            val cached = mediaUrlCache[id]
            if (cached != null) result[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return result

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.media().listWithEditContext(
                MediaListParams(include = uncached)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                for (media in response.response.data) {
                    val url = toPhotonUrl(site, media.sourceUrl)
                    mediaUrlCache[media.id] = url
                    result[media.id] = url
                }
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchMediaUrls failed: $msg"
                )
            }
        }
        return result
    }

    /**
     * Fetches display names for the given [userIds] in a single network
     * call using the `include` parameter, returning a map of user ID to
     * display name. IDs already in the local cache are returned
     * immediately without a network round-trip.
     */
    suspend fun fetchUserDisplayNames(
        site: SiteModel,
        userIds: List<Long>
    ): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in userIds) {
            val cached = userNameCache[id]
            if (cached != null) result[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return result

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.users().listWithViewContext(
                UserListParams(include = uncached)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                for (user in response.response.data) {
                    userNameCache[user.id] = user.name
                    result[user.id] = user.name
                }
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchUserDisplayNames failed: $msg"
                )
            }
        }
        return result
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
        const val FEATURED_IMAGE_SIZE_DP = 64
    }
}
