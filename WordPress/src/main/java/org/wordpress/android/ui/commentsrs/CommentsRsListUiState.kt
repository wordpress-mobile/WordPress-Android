package org.wordpress.android.ui.commentsrs

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
