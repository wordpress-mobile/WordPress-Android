package org.wordpress.android.ui.commentsrs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus

/** One comment row. */
data class CommentRsUiModel(
    val remoteCommentId: Long,
    val authorName: String,
    val avatarUrl: String,
    val snippet: String,
    val relativeDate: String,
    val status: CommentStatus,
    val postId: Long,
    /** Resolved asynchronously in a batched request; null until then. */
    val postTitle: String? = null
) {
    val isPending: Boolean get() = status == CommentStatus.UNAPPROVED
}

data class CommentsTabUiState(
    val comments: List<CommentRsUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

/** Destructive batch actions awaiting user confirmation. */
sealed interface PendingConfirmation {
    data class Trash(val commentIds: List<Long>) : PendingConfirmation
    data class Delete(val commentIds: List<Long>) : PendingConfirmation
}

/**
 * Batch moderation actions offered in selection mode, rendered as icon buttons in the contextual
 * action bar (mirroring the legacy list's action mode). Each maps to a target [CommentStatus]
 * applied to every selected comment (except [DELETE], which deletes permanently); [labelResId]
 * doubles as the icon's content description.
 */
enum class CommentsRsBatchAction(
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    val targetStatus: CommentStatus,
    val isDestructive: Boolean = false
) {
    APPROVE(R.string.mnu_comment_approve, R.drawable.ic_thumbs_up_white_24dp, CommentStatus.APPROVED),
    UNAPPROVE(R.string.mnu_comment_unapprove, R.drawable.ic_thumbs_down_white_24dp, CommentStatus.UNAPPROVED),
    SPAM(R.string.mnu_comment_spam, R.drawable.ic_spam_white_24dp, CommentStatus.SPAM),
    NOT_SPAM(R.string.mnu_comment_unspam, R.drawable.ic_spam_white_24dp, CommentStatus.APPROVED),
    TRASH(R.string.mnu_comment_trash, R.drawable.ic_trash_white_24dp, CommentStatus.TRASH, isDestructive = true),
    UNTRASH(R.string.mnu_comment_untrash, R.drawable.ic_trash_white_24dp, CommentStatus.APPROVED),
    DELETE(
        R.string.mnu_comment_delete_permanently,
        R.drawable.ic_trash_white_24dp,
        CommentStatus.DELETED,
        isDestructive = true
    )
}

/** The batch actions offered for a tab's selection, mirroring the legacy action mode. */
internal fun CommentsRsListTab.batchActions(): List<CommentsRsBatchAction> = when (this) {
    CommentsRsListTab.ALL -> listOf(
        CommentsRsBatchAction.APPROVE,
        CommentsRsBatchAction.UNAPPROVE,
        CommentsRsBatchAction.SPAM,
        CommentsRsBatchAction.TRASH
    )
    CommentsRsListTab.PENDING -> listOf(
        CommentsRsBatchAction.APPROVE,
        CommentsRsBatchAction.SPAM,
        CommentsRsBatchAction.TRASH
    )
    CommentsRsListTab.APPROVED -> listOf(
        CommentsRsBatchAction.UNAPPROVE,
        CommentsRsBatchAction.SPAM,
        CommentsRsBatchAction.TRASH
    )
    CommentsRsListTab.SPAM -> listOf(
        CommentsRsBatchAction.NOT_SPAM,
        CommentsRsBatchAction.TRASH
    )
    CommentsRsListTab.TRASHED -> listOf(
        CommentsRsBatchAction.UNTRASH,
        CommentsRsBatchAction.DELETE
    )
}
