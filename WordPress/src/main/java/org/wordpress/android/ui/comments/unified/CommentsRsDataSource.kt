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
import java.util.Date
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
        val dateGmt: Date,
        val contentHtml: String,
        val url: String,
        val postId: Long,
        val status: CommentStatus
    )

    /** Result of a write request, carrying the server error message when one is available. */
    sealed interface RsResult {
        object Success : RsResult
        data class Error(val message: String?) : RsResult
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
        authorAvatarUrl = (authorAvatarUrls[UserAvatarSize.Size96]
            ?: authorAvatarUrls.values.firstOrNull { !it.isNullOrEmpty() }).orEmpty(),
        // dateGmt is a UTC java.util.Date (an absolute instant), so relative-time formatting is
        // correct regardless of the site's timezone — unlike the offset-less local `date` field.
        dateGmt = dateGmt,
        contentHtml = content.rendered,
        url = link,
        postId = post,
        status = status.toAppCommentStatus()
    )
}

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
