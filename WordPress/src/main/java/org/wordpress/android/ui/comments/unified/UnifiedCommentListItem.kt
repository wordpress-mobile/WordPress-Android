package org.wordpress.android.ui.comments.unified

import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.SUB_HEADER

sealed class UnifiedCommentListItem(val type: CommentListItemType) {
    abstract val id: Long

    data class SubHeader(val label: String, override val id: Long) : UnifiedCommentListItem(SUB_HEADER)
    data class Comment(
        val remoteCommentId: Long,
        val postTitle: String,
        val authorName: String,
        val authorEmail: String,
        val authorAvatarUrl: String,
        val content: String,
        val publishedDate: String,
        val publishedTimestamp: Long,
        val isPending: Boolean,
        val isSelected: Boolean,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : UnifiedCommentListItem(COMMENT) {
        override val id: Long
            get() = remoteCommentId
    }

    enum class CommentListItemType {
        SUB_HEADER,
        COMMENT;
    }

    data class ToggleAction(
        val remoteCommentId: Long,
        val commentStatus: CommentStatus,
        private val toggleSelected: (remoteCommentId: Long, commentStatus: CommentStatus) -> Unit
    ) {
        fun onToggle() = toggleSelected(remoteCommentId, commentStatus)
    }

    data class ClickAction(
        val remoteCommentId: Long,
        val commentStatus: CommentStatus,
        private val clickItem: (remoteCommentId: Long, commentStatus: CommentStatus) -> Unit
    ) {
        fun onClick() = clickItem(remoteCommentId, commentStatus)
    }
}
