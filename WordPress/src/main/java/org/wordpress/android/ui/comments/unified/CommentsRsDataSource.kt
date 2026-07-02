package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CommentCreateParams
import uniffi.wp_api.CommentDeleteParams
import uniffi.wp_api.CommentListParams
import uniffi.wp_api.CommentRetrieveParams
import uniffi.wp_api.CommentUpdateParams
import uniffi.wp_api.CommentWithViewContext
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.SparseAnyPostFieldWithViewContext
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.UserAvatarSize
import uniffi.wp_api.WpApiParamCommentsOrderBy
import uniffi.wp_api.WpApiParamOrder
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.wp_api.CommentStatus as RsCommentStatus

/**
 * Comment CRUD and list paging against the wordpress-rs `/wp/v2/comments` endpoint, used by the
 * unified comment detail and rs comment list screens. Works on WP.com and self-hosted
 * application-password sites — the same sites the postsrs/pagesrs screens support.
 *
 * Liking is intentionally NOT here: wordpress-rs has no comment like action, so the detail screen
 * keeps using FluxC for likes (and for keeping the FluxC-backed comment list cache in sync).
 */
@Singleton
class CommentsRsDataSource @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider
) {
    // Keyed by (local site id, post id): post ids are only unique per site, and this is a
    // singleton shared across site switches. An empty value is a negative-cache entry for ids
    // whose title can't be resolved (custom post types, non-published posts).
    private val postTitleCache = ConcurrentHashMap<Pair<Int, Long>, String>()

    data class RsComment(
        val authorName: String,
        val authorAvatarUrl: String,
        val dateGmt: Date,
        val contentHtml: String,
        val url: String,
        val postId: Long,
        val status: CommentStatus
    )

    /** One comment list row, mapped from [CommentWithViewContext]. */
    data class RsCommentListItem(
        val remoteCommentId: Long,
        val authorName: String,
        val authorAvatarUrl: String,
        val dateGmt: Date,
        val contentHtml: String,
        val postId: Long,
        val status: CommentStatus
    )

    /** Result of a write request, carrying the server error message when one is available. */
    sealed interface RsResult {
        object Success : RsResult
        data class Error(val message: String?) : RsResult
    }

    /** Result of fetching one page of the comment list. */
    sealed interface RsCommentsPageResult {
        data class Success(
            val comments: List<RsCommentListItem>,
            /** Ready-made params for the next page; null when this was the last page. */
            val nextPageParams: CommentListParams?
        ) : RsCommentsPageResult

        data class Error(val message: String?) : RsCommentsPageResult
    }

    suspend fun getComment(site: SiteModel, commentId: Long): RsComment? = safe(errorValue = null) {
        val client = wpApiClientProvider.getWpApiClient(site)
        when (
            val result = client.request {
                it.comments().retrieveWithViewContext(commentId, CommentRetrieveParams())
            }
        ) {
            is WpRequestResult.Success -> result.response.data.toRsComment()
            else -> null
        }
    }

    /**
     * Fetches one page of the site's comments. Pass [firstPageParams] for the first page and the
     * previous page's [RsCommentsPageResult.Success.nextPageParams] for subsequent pages.
     */
    suspend fun fetchCommentsPage(site: SiteModel, params: CommentListParams): RsCommentsPageResult =
        safe(errorValue = RsCommentsPageResult.Error(null)) {
            val client = wpApiClientProvider.getWpApiClient(site)
            when (val result = client.request { it.comments().listWithViewContext(params) }) {
                is WpRequestResult.Success -> RsCommentsPageResult.Success(
                    comments = result.response.data.map { it.toRsCommentListItem() },
                    nextPageParams = result.response.nextPageParams
                )
                is WpRequestResult.WpError -> RsCommentsPageResult.Error(result.errorMessage)
                else -> RsCommentsPageResult.Error(null)
            }
        }

    fun firstPageParams(status: RsCommentStatus?, search: String? = null): CommentListParams =
        CommentListParams(
            perPage = COMMENTS_PAGE_SIZE,
            search = search,
            status = status,
            orderby = WpApiParamCommentsOrderBy.DATE_GMT,
            order = WpApiParamOrder.DESC
        )

    /**
     * Fetches post titles for the given [postIds] in batched sparse-field network calls, returning
     * a map of post id to rendered title. Ids already cached skip the network round-trip.
     * (List rows show "author commented on {post}", but the comment payload only has the post id.)
     *
     * Comments can be attached to posts or pages, so unresolved ids fall back to the pages
     * endpoint. Ids neither endpoint returns (custom post types, non-published posts) resolve to
     * an empty title and are negative-cached so they aren't re-requested on every page load.
     */
    suspend fun fetchPostTitles(site: SiteModel, postIds: List<Long>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in postIds.distinct()) {
            val cached = postTitleCache[site.id to id]
            if (cached != null) result[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return result

        safe(errorValue = Unit) {
            val unresolved = fetchTitlesFromEndpoint(site, PostEndpointType.Posts, uncached, result)
                ?: return@safe
            val stillUnresolved = if (unresolved.isEmpty()) {
                unresolved
            } else {
                fetchTitlesFromEndpoint(site, PostEndpointType.Pages, unresolved, result) ?: return@safe
            }
            // Only reached when both endpoints succeeded, so these ids genuinely have no
            // resolvable title (as opposed to a transient request failure).
            for (id in stillUnresolved) {
                postTitleCache[site.id to id] = ""
                result[id] = ""
            }
        }
        return result
    }

    /**
     * Fetches titles for [ids] from [endpointType] into [into] (and the cache), returning the ids
     * the endpoint didn't include, or null when the request failed. [PostListParams.perPage] is
     * set explicitly: the server default of 10 would silently truncate larger batches.
     */
    private suspend fun fetchTitlesFromEndpoint(
        site: SiteModel,
        endpointType: PostEndpointType,
        ids: List<Long>,
        into: MutableMap<Long, String>
    ): List<Long>? {
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.posts().filterListWithViewContext(
                endpointType,
                PostListParams(include = ids, perPage = ids.size.toUInt()),
                listOf(SparseAnyPostFieldWithViewContext.ID, SparseAnyPostFieldWithViewContext.TITLE)
            )
        }
        if (response !is WpRequestResult.Success) {
            val message = (response as? WpRequestResult.WpError<*>)?.errorMessage
            AppLog.w(AppLog.T.COMMENTS, "fetchPostTitles ($endpointType) failed: $message")
            return null
        }
        for (post in response.response.data) {
            val id = post.id ?: continue
            val title = post.title?.rendered.orEmpty()
            postTitleCache[site.id to id] = title
            into[id] = title
        }
        return ids.filter { it !in into }
    }

    suspend fun updateStatus(site: SiteModel, commentId: Long, status: CommentStatus): RsResult =
        write(site) { it.comments().update(commentId, CommentUpdateParams(status = status.toRsCommentStatus())) }

    suspend fun delete(site: SiteModel, commentId: Long): RsResult =
        write(site) { it.comments().delete(commentId, CommentDeleteParams()) }

    suspend fun createReply(
        site: SiteModel,
        postId: Long,
        parentCommentId: Long,
        content: String
    ): RsResult = write(site) {
        it.comments().create(CommentCreateParams(post = postId, parent = parentCommentId, content = content))
    }

    private suspend fun write(site: SiteModel, request: suspend (UniffiWpApiClient) -> Any): RsResult =
        safe(errorValue = RsResult.Error(null)) {
            val client = wpApiClientProvider.getWpApiClient(site)
            when (val result = client.request(request)) {
                is WpRequestResult.Success -> RsResult.Success
                is WpRequestResult.WpError -> RsResult.Error(result.errorMessage)
                else -> RsResult.Error(null)
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <R> safe(errorValue: R, block: suspend () -> R): R = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(AppLog.T.COMMENTS, "wordpress-rs comment request failed", e)
        errorValue
    }

    private fun CommentWithViewContext.toRsComment() = RsComment(
        authorName = authorName,
        authorAvatarUrl = pickAvatarUrl(),
        // dateGmt is a UTC java.util.Date (an absolute instant), so relative-time formatting is
        // correct regardless of the site's timezone — unlike the offset-less local `date` field.
        dateGmt = dateGmt,
        contentHtml = content.rendered,
        url = link,
        postId = post,
        status = status.toAppCommentStatus()
    )

    companion object {
        internal const val COMMENTS_PAGE_SIZE = 30u
    }
}

internal fun CommentWithViewContext.toRsCommentListItem() = CommentsRsDataSource.RsCommentListItem(
    remoteCommentId = id,
    authorName = authorName,
    authorAvatarUrl = pickAvatarUrl(),
    dateGmt = dateGmt,
    contentHtml = content.rendered,
    postId = post,
    status = status.toAppCommentStatus()
)

internal fun CommentWithViewContext.pickAvatarUrl(): String =
    (authorAvatarUrls[UserAvatarSize.Size96] ?: authorAvatarUrls.values.firstOrNull { !it.isNullOrEmpty() }).orEmpty()

internal fun CommentStatus.toRsCommentStatus(): RsCommentStatus = when (this) {
    CommentStatus.APPROVED -> RsCommentStatus.Approved
    CommentStatus.UNAPPROVED -> RsCommentStatus.Hold
    CommentStatus.SPAM -> RsCommentStatus.Spam
    CommentStatus.TRASH -> RsCommentStatus.Trash
    else -> RsCommentStatus.Approved
}

internal fun RsCommentStatus.toAppCommentStatus(): CommentStatus = when (this) {
    is RsCommentStatus.Approved -> CommentStatus.APPROVED
    is RsCommentStatus.Hold -> CommentStatus.UNAPPROVED
    is RsCommentStatus.Spam -> CommentStatus.SPAM
    is RsCommentStatus.Trash -> CommentStatus.TRASH
    else -> CommentStatus.ALL
}
