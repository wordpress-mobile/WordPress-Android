package org.wordpress.android.ui.rs

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CommentListParams
import uniffi.wp_api.SparseCommentFieldWithViewContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts the comments on posts, for the redesigned posts and pages lists.
 *
 * Reads the plain WP REST comments endpoint rather than WordPress.com stats, so it works the same
 * on a self-hosted site reached over an application password as it does on a WordPress.com site -
 * [WpApiClientProvider] picks the transport and the call is identical either way. View counts have
 * no such equivalent and remain WordPress.com-only.
 *
 * Counts approved comments, matching the number wp-admin shows against a post.
 */
@Singleton
class RsCommentCountFetcher @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider
) {
    /**
     * Comment counts for [postIds], keyed by post id.
     *
     * Normally one request for the whole batch. Posts absent from the response have no comments and
     * come back as zero, so a returned map covers every id it was asked for.
     */
    suspend fun fetchCommentCounts(site: SiteModel, postIds: List<Long>): Map<Long, Long> {
        if (postIds.isEmpty()) return emptyMap()
        return when (val outcome = fetchAsBatch(site, postIds)) {
            is BatchOutcome.Counted -> outcome.counts
            // Only worth the per-post fan-out when the batch actually reached the site and simply
            // could not fit. A failed request would just fail N more times, holding its callers up
            // for N timeouts to end up in the same place.
            BatchOutcome.Incomplete -> fetchOneByOne(site, postIds)
            BatchOutcome.Failed -> emptyMap()
        }
    }

    /** What a batched attempt produced, and so whether falling back is worth the requests. */
    private sealed interface BatchOutcome {
        data class Counted(val counts: Map<Long, Long>) : BatchOutcome

        /** The site answered, but with more comments than one page holds. */
        data object Incomplete : BatchOutcome

        /** The request did not reach the site, or it errored. */
        data object Failed : BatchOutcome
    }

    /** One request covering every post, counted client-side. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAsBatch(site: SiteModel, postIds: List<Long>): BatchOutcome {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val result = client.request { api ->
                api.comments().filterListWithViewContext(
                    CommentListParams(post = postIds, perPage = BATCH_PAGE_SIZE),
                    listOf(
                        SparseCommentFieldWithViewContext.ID,
                        SparseCommentFieldWithViewContext.POST
                    )
                )
            }
            val success = result as? WpRequestResult.Success
                ?: return BatchOutcome.Failed
            val comments = success.response.data
            val total = success.response.headerMap.wpTotal()?.toInt()
            // More comments exist than came back, so counting what did would undercount.
            if (total != null && total > comments.size) {
                BatchOutcome.Incomplete
            } else {
                BatchOutcome.Counted(tally(postIds, comments.mapNotNull { it.post }))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.COMMENTS, "Batched comment count failed", e)
            BatchOutcome.Failed
        }
    }

    /**
     * Counts how often each id appears in [commentPostIds].
     *
     * Every id in [postIds] is seeded at zero: a post with no comments is simply absent from the
     * response, and "no comments" is a real answer rather than a missing one.
     */
    private fun tally(postIds: List<Long>, commentPostIds: List<Long>): Map<Long, Long> {
        val counts = postIds.associateWith { 0L }.toMutableMap()
        commentPostIds.forEach { postId ->
            counts[postId]?.let { counts[postId] = it + 1 }
        }
        return counts
    }

    /**
     * Fallback for posts busy enough to overflow a single page: ask per post for a single comment
     * and read the count out of the total header, so the body stays small and the number is exact.
     */
    private suspend fun fetchOneByOne(site: SiteModel, postIds: List<Long>): Map<Long, Long> {
        val counts = mutableMapOf<Long, Long>()
        postIds.forEach { postId ->
            countForPost(site, postId)?.let { counts[postId] = it }
        }
        return counts
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun countForPost(site: SiteModel, postId: Long): Long? {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val result = client.request { api ->
                api.comments().filterListWithViewContext(
                    CommentListParams(post = listOf(postId), perPage = SINGLE_PAGE_SIZE),
                    listOf(SparseCommentFieldWithViewContext.ID)
                )
            }
            (result as? WpRequestResult.Success)?.response?.headerMap?.wpTotal()?.toLong()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.COMMENTS, "Comment count failed for post $postId", e)
            null
        }
    }

    companion object {
        /** WP REST caps a page at 100, which is also the most a batch can count without paging. */
        private const val BATCH_PAGE_SIZE = 100u

        /** The fallback only needs the total header, so it asks for the smallest body possible. */
        private const val SINGLE_PAGE_SIZE = 1u
    }
}
