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

/** A destructive batch [action] awaiting user confirmation, to be applied to [commentIds]. */
data class PendingConfirmation(val action: CommentsRsBatchAction, val commentIds: List<Long>)

/**
 * Confirmation-dialog copy for a destructive batch action. [messagePluralResId] is shown when more
 * than one comment is selected, otherwise [messageResId].
 */
data class ConfirmationCopy(
    @StringRes val titleResId: Int,
    @StringRes val confirmButtonResId: Int,
    @StringRes val messageResId: Int,
    @StringRes val messagePluralResId: Int = messageResId
)

/**
 * Batch moderation actions offered in selection mode. Each maps to a target [CommentStatus]
 * applied to every selected comment (except [DELETE], which deletes permanently); [labelResId]
 * doubles as the icon's content description. Mirroring the legacy list's action mode, only the
 * [showAsIcon] actions (approve/unapprove) appear as top-bar icon buttons; the rest fall into the
 * overflow menu. An action with [confirmation] copy is destructive: it asks before running and is
 * tinted accordingly.
 */
enum class CommentsRsBatchAction(
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    val targetStatus: CommentStatus,
    val showAsIcon: Boolean = false,
    val confirmation: ConfirmationCopy? = null
) {
    APPROVE(
        R.string.mnu_comment_approve,
        R.drawable.ic_thumbs_up_white_24dp,
        CommentStatus.APPROVED,
        showAsIcon = true
    ),
    UNAPPROVE(
        R.string.mnu_comment_unapprove,
        R.drawable.ic_thumbs_down_white_24dp,
        CommentStatus.UNAPPROVED,
        showAsIcon = true
    ),
    SPAM(R.string.mnu_comment_spam, R.drawable.ic_spam_white_24dp, CommentStatus.SPAM),
    NOT_SPAM(R.string.mnu_comment_unspam, R.drawable.ic_spam_white_24dp, CommentStatus.APPROVED),
    TRASH(
        R.string.mnu_comment_trash,
        R.drawable.ic_trash_white_24dp,
        CommentStatus.TRASH,
        confirmation = ConfirmationCopy(
            titleResId = R.string.trash,
            confirmButtonResId = R.string.dlg_confirm_action_trash,
            messageResId = R.string.dlg_confirm_trash_comments
        )
    ),
    UNTRASH(R.string.mnu_comment_untrash, R.drawable.ic_trash_white_24dp, CommentStatus.APPROVED),
    DELETE(
        R.string.mnu_comment_delete_permanently,
        R.drawable.ic_trash_white_24dp,
        CommentStatus.DELETED,
        confirmation = ConfirmationCopy(
            titleResId = R.string.delete,
            confirmButtonResId = R.string.delete,
            messageResId = R.string.dlg_sure_to_delete_comment,
            messagePluralResId = R.string.dlg_sure_to_delete_comments
        )
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
    // Unreplied rows are approved or pending comments, so offer the same actions as ALL.
    CommentsRsListTab.UNREPLIED -> listOf(
        CommentsRsBatchAction.APPROVE,
        CommentsRsBatchAction.UNAPPROVE,
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

/**
 * Whether [this] action can act on a selection whose comments have [selectedStatuses]. Every batch
 * action requires [canModerate] (the moderate_comments capability) — without it they stay visible
 * but disabled, mirroring the detail screen's footer. Approve and unapprove are additionally
 * disabled when they would be a no-op — nothing unapproved to approve, or nothing approved to
 * unapprove; every other action is enabled whenever the user can moderate.
 */
internal fun CommentsRsBatchAction.isEnabledFor(
    selectedStatuses: Set<CommentStatus>,
    canModerate: Boolean
): Boolean = canModerate && when (this) {
    CommentsRsBatchAction.APPROVE -> CommentStatus.UNAPPROVED in selectedStatuses
    CommentsRsBatchAction.UNAPPROVE -> CommentStatus.APPROVED in selectedStatuses
    else -> true
}
