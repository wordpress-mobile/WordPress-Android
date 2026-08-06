package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.SparseAnyPostFieldWithViewContext
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

        // Only the ID is needed; without the field filter the response carries the post's whole
        // rendered content, excerpt and taxonomy payload.
        val result = wpApiClientProvider
            .getWpApiClient(site)
            .request { requestBuilder ->
                requestBuilder.posts()
                    .filterListWithViewContext(
                        postEndpointType =
                            PostEndpointType.Posts,
                        params = params,
                        fields = listOf(
                            SparseAnyPostFieldWithViewContext
                                .ID
                        )
                    )
            }

        return when (result) {
            is WpRequestResult.Success ->
                result.response.data
                    .firstOrNull()?.id
                    ?.let {
                        LatestPostLookupResult.Success(it)
                    }
                    ?: LatestPostLookupResult.NoPosts
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
}

/**
 * Result of the latest-post lookup. [NoPosts] is a success -- the site simply has nothing
 * published yet -- and is shown as an empty state rather than an error.
 */
sealed class LatestPostLookupResult {
    data class Success(
        val postId: Long
    ) : LatestPostLookupResult()

    data object NoPosts : LatestPostLookupResult()

    data class Error(
        val message: String
    ) : LatestPostLookupResult()
}
