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
        return fetchAsBatch(site, postIds) ?: fetchOneByOne(site, postIds)
    }

    /**
     * One request covering every post, counted client-side.
     *
     * Returns null when the batch cannot be counted this way - either the request failed, or the
     * posts between them have more comments than a single page holds, which would silently
     * undercount the busiest of them.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAsBatch(site: SiteModel, postIds: List<Long>): Map<Long, Long>? {
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
            if (result !is WpRequestResult.Success) return null

            val comments = result.response.data
            val total = result.response.headerMap.wpTotal()?.toInt()
            // More comments exist than came back, so counting what did would undercount.
            if (total != null && total > comments.size) return null

            // Seed every requested id: a post with no comments is simply absent from the response,
            // and "no comments" is a real answer rather than a missing one.
            val counts = postIds.associateWith { 0L }.toMutableMap()
            comments.forEach { comment ->
                comment.post?.let { postId ->
                    counts[postId]?.let { counts[postId] = it + 1 }
                }
            }
            counts
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.COMMENTS, "Batched comment count failed", e)
            null
        }
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
            if (result !is WpRequestResult.Success) return null
            result.response.headerMap.wpTotal()?.toLong()
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
