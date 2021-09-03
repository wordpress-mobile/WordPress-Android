package org.wordpress.android.ui.comments

import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.ui.comments.CommentListItem.CommentListItemType.COMMENT
import org.wordpress.android.ui.comments.CommentListItem.CommentListItemType.HEADER

@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
sealed class CommentListItem(val type: CommentListItemType) {
    abstract val id: Long
    data class SubHeader(val label: String, override val id: Long) : CommentListItem(HEADER)
    data class Comment(val comment: CommentModel) : CommentListItem(COMMENT) {
        override val id: Long
            get() = comment.remoteCommentId
    }

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
