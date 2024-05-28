@file:Suppress("DEPRECATION")
package org.wordpress.android.ui.comments

import androidx.core.view.isVisible
import org.wordpress.android.databinding.CommentPendingBinding
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.models.Note


/**
 * Used when we want to write Kotlin in [CommentDetailFragment]
 * Move converted code to this class
 */
abstract class SharedCommentDetailFragment:CommentDetailFragment() {
    protected val note: Note // it will be non-null when users from a notification
        get() = mNote!!

    protected val comment: CommentModel
        get() = mComment!!

    protected fun updateModerationStatus() {
        val commentStatus = CommentStatus.fromString(comment.status)
        when(commentStatus){
            CommentStatus.APPROVED -> TODO()
            CommentStatus.UNAPPROVED -> mBinding?.layoutCommentPending?.handlePendingView()
            CommentStatus.SPAM -> TODO()
            CommentStatus.TRASH -> TODO()
            CommentStatus.DELETED -> TODO()
            CommentStatus.ALL -> TODO()
            CommentStatus.UNREPLIED -> TODO()
            CommentStatus.UNSPAM -> TODO()
            CommentStatus.UNTRASH -> TODO()
        }
    }

    private fun CommentPendingBinding.handlePendingView() {
        layoutRoot.isVisible = true
    }
}
