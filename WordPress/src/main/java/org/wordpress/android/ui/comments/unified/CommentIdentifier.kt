package org.wordpress.android.ui.comments.unified

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CommentIdentifier : Parcelable {
    @Parcelize
    data class SiteCommentIdentifier(
        val localCommentId: Int
    ) : CommentIdentifier()

    @Parcelize
    data class ReaderCommentIdentifier(
        val blogId: Long,
        val postId: Long,
        val remoteCommentId: Long
    ) : CommentIdentifier()

    @Parcelize
    data class NotificationCommentIdentifier(
        val remoteCommentId: Long
    ) : CommentIdentifier()
}
