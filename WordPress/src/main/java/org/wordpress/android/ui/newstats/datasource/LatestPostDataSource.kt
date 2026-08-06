package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaId
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.SparseAnyPostFieldWithViewContext
import uniffi.wp_api.SparseMediaFieldWithViewContext
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamPostsOrderBy
import javax.inject.Inject

/**
 * Looks up the site's most recently published post.
 *
 * The stats API needs a post ID before it can return per-post views, and the WP.com stats surface
 * has no posts endpoint of its own, so this goes through the site's REST API instead.
 *
 * [WpApiClientProvider.getWpApiClient] routes WP.com and Jetpack sites through the WP.com REST
 * proxy using the account's OAuth token, so no application password is needed for the sites that
 * have stats in the first place.
 */
class LatestPostDataSource @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider
) {
    suspend fun fetchLatestPublishedPost(
        site: SiteModel
    ): LatestPostLookupResult {
        val params = PostListParams(
            perPage = 1u,
            order = WpApiParamOrder.DESC,
            orderby = WpApiParamPostsOrderBy.DATE,
            status = listOf(PostStatus.Publish)
        )

        // Only the id and featured image are needed; without the field filter the response
        // carries the post's whole rendered content, excerpt and taxonomy payload.
        val client = wpApiClientProvider.getWpApiClient(site)
        val result = client.request { requestBuilder ->
            requestBuilder.posts()
                .filterListWithViewContext(
                    postEndpointType =
                        PostEndpointType.Posts,
                    params = params,
                    fields = listOf(
                        SparseAnyPostFieldWithViewContext.ID,
                        SparseAnyPostFieldWithViewContext
                            .FEATURED_MEDIA
                    )
                )
        }

        return when (result) {
            is WpRequestResult.Success -> {
                val post = result.response.data.firstOrNull()
                val postId = post?.id
                if (postId == null) {
                    LatestPostLookupResult.NoPosts
                } else {
                    LatestPostLookupResult.Success(
                        postId = postId,
                        featuredImageUrl = post.featuredMedia
                            ?.let { fetchImageUrl(client, it) }
                    )
                }
            }
            else -> {
                val message = (
                    result as? WpRequestResult.WpError<*>
                    )?.errorMessage
                    ?: "Failed to fetch the latest post"
                AppLog.e(
                    T.STATS,
                    "LatestPostDataSource: " +
                        "fetchLatestPublishedPost " +
                        "failed - $message"
                )
                LatestPostLookupResult.Error(message)
            }
        }
    }

    /**
     * Resolves a featured image's URL. The post carries only the attachment id, so this is a
     * second call -- made only when there is an image. A failure here costs the thumbnail, not
     * the card, so it degrades to null rather than propagating.
     */
    private suspend fun fetchImageUrl(
        client: WpApiClient,
        mediaId: MediaId
    ): String? {
        val result = client.request { requestBuilder ->
            requestBuilder.media()
                .filterRetrieveWithViewContext(
                    mediaId = mediaId,
                    fields = listOf(
                        SparseMediaFieldWithViewContext
                            .SOURCE_URL
                    )
                )
        }
        return when (result) {
            is WpRequestResult.Success ->
                result.response.data.sourceUrl
            else -> {
                AppLog.w(
                    T.STATS,
                    "LatestPostDataSource: could not " +
                        "resolve featured image $mediaId"
                )
                null
            }
        }
    }
}

/**
 * Result of the latest-post lookup. [NoPosts] is a success -- the site simply has nothing
 * published yet -- and is shown as an empty state rather than an error.
 */
sealed class LatestPostLookupResult {
    data class Success(
        val postId: Long,
        /** Null when the post has no featured image, or when resolving its URL failed. */
        val featuredImageUrl: String?
    ) : LatestPostLookupResult()

    data object NoPosts : LatestPostLookupResult()

    data class Error(
        val message: String
    ) : LatestPostLookupResult()
}
