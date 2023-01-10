package org.wordpress.android.ui.comments.unified

import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.CommentListItemType.NEXT_PAGE_LOADER
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

    data class NextPageLoader(val isLoading: Boolean, override val id: Long, val loadAction: () -> Unit) :
        UnifiedCommentListItem(
            NEXT_PAGE_LOADER
        )

    enum class CommentListItemType {
        SUB_HEADER,
        COMMENT,
        NEXT_PAGE_LOADER;
    }

    data class ToggleAction(
        val comment: CommentEntity,
        private val toggleSelected: (remoteCommentId: Long, commentStatus: CommentStatus) -> Unit
    ) {
        fun onToggle() = toggleSelected(comment.remoteCommentId, CommentStatus.fromString(comment.status))
    }

    data class ClickAction(
        val comment: CommentEntity,
        private val clickItem: (comment: CommentEntity) -> Unit
    ) {
        fun onClick() = clickItem(comment)
    }
}
