package org.wordpress.android.ui.reader.views.uistates

import org.wordpress.android.ui.reader.views.uistates.CommentItemType.BUTTON
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.COMMENT
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.LOADING
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.TEXT_MESSAGE
import org.wordpress.android.ui.utils.UiString

enum class CommentItemType {
    LOADING,
    COMMENT,
    BUTTON,
    TEXT_MESSAGE
}

sealed class CommentSnippetItemState(open val type: CommentItemType) {
    object LoadingState : CommentSnippetItemState(LOADING)

    data class CommentState(
        val authorName: String,
        val datePublished: String,
        val avatarUrl: String,
        val showAuthorBadge: Boolean,
        val commentText: String,
        val isPrivatePost: Boolean,
        val blogId: Long,
        val postId: Long,
        val commentId: Long
    ) : CommentSnippetItemState(COMMENT)

    data class ButtonState(
        val buttonText: UiString,
        val postId: Long,
        val blogId: Long,
        val onCommentSnippetClicked: (Long, Long) -> Unit
    ) : CommentSnippetItemState(BUTTON)

    data class TextMessage(
        val message: UiString
    ) : CommentSnippetItemState(TEXT_MESSAGE)
}
