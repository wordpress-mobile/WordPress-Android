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
    // it will be non-null after view created when users from a notification
    // it will be null when users from comment list
    protected val note: Note
        get() = mNote!!

    // it will be non-null after view created when users from comment list
    // it will be non-null in a different time point when users from a notification
    protected val comment: CommentModel
        get() = mComment!!

    override fun updateModerationStatus() {
        val commentStatus = CommentStatus.fromString(comment.status)
        when(commentStatus){
            CommentStatus.APPROVED -> {}
            CommentStatus.UNAPPROVED -> mBinding?.layoutCommentPending?.handlePendingView()
            CommentStatus.SPAM -> {}
            CommentStatus.TRASH -> {}
            CommentStatus.DELETED -> {}
            CommentStatus.ALL -> {}
            CommentStatus.UNREPLIED -> {}
            CommentStatus.UNSPAM -> {}
            CommentStatus.UNTRASH -> {}
        }

        mBinding?.layoutCommentPending?.handlePendingView() // todo: remove this line after PR review
    }

    private fun CommentPendingBinding.handlePendingView() {
        layoutRoot.isVisible = true
        textMoreOptions.setOnClickListener { showModerationBottomSheet() }
    }

    private fun showModerationBottomSheet() {
        ModerationBottomSheetDialogFragment.newInstance()
            .show(childFragmentManager, ModerationBottomSheetDialogFragment.TAG)
    }
}
