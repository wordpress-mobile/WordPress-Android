package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named

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
    private val latestPostInsightsRestClient: LatestPostInsightsRestClient,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Runs on [ioDispatcher]: issuing the request reaches the response cache on the calling
     * thread, and a forced fetch reads and rewrites the cached entry there. The other callers
     * of this client wrap it the same way.
     */
    suspend fun fetchLatestPublishedPost(
        site: SiteModel
    ): LatestPostLookupResult = withContext(ioDispatcher) {
        // Forced: the card loads once per screen entry and on pull-to-refresh, and
        // LatestPostViewModel tracks that itself, so the response cache underneath would only
        // serve a post this screen has already decided to replace.
        val payload = latestPostInsightsRestClient
            .fetchLatestPostForInsights(site, forced = true)

        val post = payload.response?.posts?.firstOrNull()
        when {
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
            // A success carrying no body at all is a failed fetch, not an empty site: saying
            // "nothing published yet" here would offer a create-post prompt instead of a retry.
            payload.response == null -> {
                AppLog.e(
                    T.STATS,
                    "LatestPostDataSource: " +
                        "fetchLatestPublishedPost " +
                        "returned an empty response"
                )
                LatestPostLookupResult.Error(
                    "Failed to fetch the latest post"
                )
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
