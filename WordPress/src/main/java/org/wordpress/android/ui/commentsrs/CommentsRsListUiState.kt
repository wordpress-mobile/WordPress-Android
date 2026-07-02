package org.wordpress.android.ui.commentsrs

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

data class ConfirmationDialogState(
    val pending: PendingConfirmation? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * Batch moderation actions offered in selection mode. Each maps to a target [CommentStatus]
 * applied to every selected comment (except [DELETE], which deletes permanently).
 */
enum class CommentsRsBatchAction(
    @StringRes val labelResId: Int,
    val targetStatus: CommentStatus,
    val isDestructive: Boolean = false
) {
    APPROVE(R.string.mnu_comment_approve, CommentStatus.APPROVED),
    UNAPPROVE(R.string.mnu_comment_unapprove, CommentStatus.UNAPPROVED),
    SPAM(R.string.mnu_comment_spam, CommentStatus.SPAM),
    NOT_SPAM(R.string.mnu_comment_unspam, CommentStatus.APPROVED),
    TRASH(R.string.mnu_comment_trash, CommentStatus.TRASH, isDestructive = true),
    UNTRASH(R.string.mnu_comment_untrash, CommentStatus.APPROVED),
    DELETE(R.string.mnu_comment_delete_permanently, CommentStatus.DELETED, isDestructive = true);
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
