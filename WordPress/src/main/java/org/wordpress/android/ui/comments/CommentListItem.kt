package org.wordpress.android.ui.comments

import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.ui.comments.CommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.CommentListItem.CommentListItemType.HEADER

sealed class CommentListItem(val type: CommentListItemType) {
    open fun longId(): Long = hashCode().toLong()

    data class SubHeader(val label: String) : CommentListItem(HEADER)
    data class Comment(val comment: CommentModel) : CommentListItem(COMMENT)

    enum class CommentListItemType {
        HEADER,
        COMMENT;

        companion object {
            @JvmStatic
            fun fromOrdinal(ordinal: Int): CommentListItemType {
                return when (ordinal) {
                    HEADER.ordinal -> HEADER
                    COMMENT.ordinal -> COMMENT
                    else -> throw IllegalArgumentException("Illegal CommentListItem ordinal $ordinal")
                }
            }
        }
    }
}
