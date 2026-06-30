package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CommentCreateParams
import uniffi.wp_api.CommentDeleteParams
import uniffi.wp_api.CommentRetrieveParams
import uniffi.wp_api.CommentUpdateParams
import uniffi.wp_api.CommentWithViewContext
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.UserAvatarSize
import javax.inject.Inject
import uniffi.wp_api.CommentStatus as RsCommentStatus

/**
 * Comment CRUD against the wordpress-rs `/wp/v2/comments` endpoint, used by the unified comment
 * detail screen. Works on WP.com and self-hosted application-password sites — the same sites the
 * postsrs/pagesrs screens support.
 *
 * Liking is intentionally NOT here: wordpress-rs has no comment like action, so the detail screen
 * keeps using FluxC for likes (and for keeping the FluxC-backed comment list cache in sync).
 */
class CommentsRsDataSource @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider
) {
    data class RsComment(
        val authorName: String,
        val authorAvatarUrl: String,
        val date: String,
        val contentHtml: String,
        val url: String,
        val postId: Long,
        val parentId: Long,
        val status: CommentStatus
    )

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

    suspend fun updateStatus(site: SiteModel, commentId: Long, status: CommentStatus): Boolean =
        write(site) { it.comments().update(commentId, CommentUpdateParams(status = status.toRsStatus())) }

    suspend fun trash(site: SiteModel, commentId: Long): Boolean =
        write(site) { it.comments().trash(commentId, CommentDeleteParams()) }

    suspend fun delete(site: SiteModel, commentId: Long): Boolean =
        write(site) { it.comments().delete(commentId, CommentDeleteParams()) }

    suspend fun createReply(
        site: SiteModel,
        postId: Long,
        parentCommentId: Long,
        content: String
    ): Boolean = write(site) {
        it.comments().create(CommentCreateParams(post = postId, parent = parentCommentId, content = content))
    }

    /** Runs a write request; returns true on error, matching the ViewModel's isError convention. */
    private suspend fun write(site: SiteModel, request: suspend (UniffiWpApiClient) -> Any): Boolean =
        safe(errorValue = true) {
            val client = wpApiClientProvider.getWpApiClient(site)
            client.request(request) !is WpRequestResult.Success
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
        authorAvatarUrl = (authorAvatarUrls[UserAvatarSize.Size96]
            ?: authorAvatarUrls.values.firstOrNull { !it.isNullOrEmpty() }).orEmpty(),
        date = date,
        contentHtml = content.rendered,
        url = link,
        postId = post,
        parentId = parent,
        status = status.toAppStatus()
    )

    private fun CommentStatus.toRsStatus(): RsCommentStatus = when (this) {
        CommentStatus.APPROVED -> RsCommentStatus.Approved
        CommentStatus.UNAPPROVED -> RsCommentStatus.Hold
        CommentStatus.SPAM -> RsCommentStatus.Spam
        CommentStatus.TRASH -> RsCommentStatus.Trash
        else -> RsCommentStatus.Approved
    }

    private fun RsCommentStatus.toAppStatus(): CommentStatus = when (this) {
        is RsCommentStatus.Approved -> CommentStatus.APPROVED
        is RsCommentStatus.Hold -> CommentStatus.UNAPPROVED
        is RsCommentStatus.Spam -> CommentStatus.SPAM
        is RsCommentStatus.Trash -> CommentStatus.TRASH
        else -> CommentStatus.ALL
    }
}
