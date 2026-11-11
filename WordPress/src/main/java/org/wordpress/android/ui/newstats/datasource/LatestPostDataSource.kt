package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

/**
 * Looks up the site's most recently published post.
 *
 * The stats API needs a post ID before it can return per-post views, and the WP.com stats surface
 * has no posts endpoint of its own, so this goes to the WP.com REST posts endpoint for the site.
 *
 * That endpoint is addressed by the site's WP.com id and authenticated with the account's OAuth
 * token -- exactly what every other card on this screen already depends on, and what opening Stats
 * at all requires. Going to the site's own REST API instead would ask for something Stats does not
 * guarantee: a site reachable over the WP.com REST proxy or holding an application password. A
 * Jetpack site whose record was built over XML-RPC has neither, and that lookup could not even
 * build a client for it (CMM-2769).
 *
 * One request carries both the post id and its featured image URL.
 */
class LatestPostDataSource @Inject constructor(
    private val latestPostInsightsRestClient: LatestPostInsightsRestClient
) {
    suspend fun fetchLatestPublishedPost(
        site: SiteModel
    ): LatestPostLookupResult {
        // Forced: the card loads once per screen entry and on pull-to-refresh, and
        // LatestPostViewModel tracks that itself, so there is nothing for the HTTP cache
        // underneath to add.
        val payload = latestPostInsightsRestClient
            .fetchLatestPostForInsights(site, forced = true)

        val post = payload.response?.posts?.firstOrNull()
        return when {
            payload.isError -> {
                val message = payload.error?.message
                    ?: "Failed to fetch the latest post"
                AppLog.e(
                    T.STATS,
                    "LatestPostDataSource: " +
                        "fetchLatestPublishedPost " +
                        "failed - $message"
                )
                LatestPostLookupResult.Error(message)
            }
            post == null -> LatestPostLookupResult.NoPosts
            else -> LatestPostLookupResult.Success(
                postId = post.id,
                // Absent rather than empty is what "no featured image" looks like here, but the
                // field is a plain string, so treat a blank one the same way.
                featuredImageUrl = post.featuredImage
                    ?.takeIf { it.isNotEmpty() }
            )
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
        /** Null when the post has no featured image. */
        val featuredImageUrl: String?
    ) : LatestPostLookupResult()

    data object NoPosts : LatestPostLookupResult()

    data class Error(
        val message: String
    ) : LatestPostLookupResult()
}
