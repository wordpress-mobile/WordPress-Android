package org.wordpress.android.ui.comments.unified

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CommentIdentifier : Parcelable {
    abstract val remoteCommentId: Long

    @Parcelize
    data class SiteCommentIdentifier(
        val localCommentId: Int,
        override val remoteCommentId: Long
    ) : CommentIdentifier()

    @Parcelize
    data class ReaderCommentIdentifier(
        val blogId: Long,
        val postId: Long,
        override val remoteCommentId: Long
    ) : CommentIdentifier()

    @Parcelize
    data class NotificationCommentIdentifier(
        val noteId: String,
        override val remoteCommentId: Long
    ) : CommentIdentifier()
}
