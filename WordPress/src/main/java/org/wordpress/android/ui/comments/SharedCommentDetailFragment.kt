@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import androidx.core.view.isVisible
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.types.Email
import org.wordpress.android.databinding.CommentPendingBinding
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.EnabledActions
import org.wordpress.android.ui.comments.CommentExtension.canLike
import org.wordpress.android.ui.comments.CommentExtension.canMarkAsSpam
import org.wordpress.android.ui.comments.CommentExtension.canModerate
import org.wordpress.android.ui.comments.CommentExtension.canReply
import org.wordpress.android.ui.comments.CommentExtension.canTrash
import org.wordpress.android.ui.comments.CommentExtension.liked
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPAvatarUtils
import java.util.EnumSet


/**
 * Used when we want to write Kotlin in [CommentDetailFragment]
 * Move converted code to this class
 */
abstract class SharedCommentDetailFragment : CommentDetailFragment() {
    // it will be non-null after view created when users from a notification
    // it will be null when users from comment list
    protected val note: Note
        get() = mNote!!

    // it will be non-null after view created when users from comment list
    // it will be non-null in a different time point when users from a notification
    protected val comment: CommentModel
        get() = mComment!!

    abstract val enabledActions: EnumSet<EnabledActions>

    override fun updateModerationStatus() {
        val commentStatus = CommentStatus.fromString(comment.status)
        when (commentStatus) {
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
    }

    private fun CommentPendingBinding.handlePendingView() {
        layoutRoot.isVisible = true
        textMoreOptions.setOnClickListener { showModerationBottomSheet() }
    }

    private fun showModerationBottomSheet() {
        ModerationBottomSheetDialogFragment.newInstance(
            ModerationBottomSheetDialogFragment.CommentState(
                canModerate = enabledActions.canModerate(),
                canMarkAsSpam = enabledActions.canMarkAsSpam(),
                canTrash = enabledActions.canTrash(),
            )
        ).show(childFragmentManager, ModerationBottomSheetDialogFragment.TAG)
    }
}

object CommentExtension {
    /**
     * Have permission to moderate/reply/spam this comment
     */
    fun EnumSet<EnabledActions>.canModerate(): Boolean = contains(EnabledActions.ACTION_APPROVE)
            || contains(EnabledActions.ACTION_UNAPPROVE)

    fun EnumSet<EnabledActions>.canMarkAsSpam(): Boolean = contains(EnabledActions.ACTION_SPAM)

    fun EnumSet<EnabledActions>.canReply(): Boolean = contains(EnabledActions.ACTION_REPLY)

    fun EnumSet<EnabledActions>.canTrash(): Boolean = canModerate()

    fun canEdit(site: SiteModel): Boolean = site.hasCapabilityEditOthersPosts || site.isSelfHostedAdmin

    fun EnumSet<EnabledActions>.canLike(site: SiteModel): Boolean =
        (contains(EnabledActions.ACTION_LIKE_COMMENT) && SiteUtils.isAccessedViaWPComRest(site))

    fun CommentModel.getAvatarUrl(size: Int): String = when {
        authorProfileImageUrl != null -> WPAvatarUtils.rewriteAvatarUrl(
            authorProfileImageUrl!!,
            size
        )

        authorEmail != null -> AvatarUrl(
            Email(authorEmail!!),
            AvatarQueryOptions(size, null, null, null)
        ).url().toString()

        else -> ""
    }

    fun CommentModel.liked() = this.iLike
}
