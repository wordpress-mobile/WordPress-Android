@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.types.Email
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.CommentApprovedBinding
import org.wordpress.android.databinding.CommentPendingBinding
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.EnabledActions
import org.wordpress.android.ui.comments.CommentExtension.canMarkAsSpam
import org.wordpress.android.ui.comments.CommentExtension.canModerate
import org.wordpress.android.ui.comments.CommentExtension.canTrash
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.image.ImageType
import java.util.EnumSet
import javax.inject.Inject


/**
 * Used when we want to write Kotlin in [CommentDetailFragment]
 * Move converted code to this class
 */
@AndroidEntryPoint
abstract class SharedCommentDetailFragment : CommentDetailFragment() {
    // it will be non-null after view created when users from a notification
    // it will be null when users from comment list
    protected val note: Note
        get() = mNote!!

    // it will be non-null after view created when users from comment list
    // it will be non-null in a different time point when users from a notification
    protected val comment: CommentModel
        get() = mComment!!

    protected val site: SiteModel
        get() = mSite!!

    protected val viewModel: CommentDetailViewModel by viewModels()

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    abstract val enabledActions: EnumSet<EnabledActions>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.updatedComment.observe(viewLifecycleOwner) {
            mComment = it
            updateModerationStatus()
        }
    }

    override fun updateModerationStatus() {
        // reset visibilities
        mBinding?.layoutCommentPending?.root?.isVisible = false
        mBinding?.layoutCommentApproved?.root?.isVisible = false

        val commentStatus = CommentStatus.fromString(comment.status)
        when (commentStatus) {
            CommentStatus.APPROVED -> mBinding?.layoutCommentApproved?.bindApprovedView()
            CommentStatus.UNAPPROVED -> mBinding?.layoutCommentPending?.bindPendingView()
            CommentStatus.SPAM -> {}
            CommentStatus.TRASH -> {}
            CommentStatus.DELETED -> {}
            CommentStatus.ALL -> {}
            CommentStatus.UNREPLIED -> {}
            CommentStatus.UNSPAM -> {}
            CommentStatus.UNTRASH -> {}
        }
    }

    private fun CommentPendingBinding.bindPendingView() {
        root.isVisible = true
        textMoreOptions.setOnClickListener { showModerationBottomSheet() }
    }

    private fun CommentApprovedBinding.bindApprovedView() {
        root.isVisible = true
        meGravatarLoader.load(
            newAvatarUploaded = false,
            avatarUrl = meGravatarLoader.constructGravatarUrl(accountStore.account.avatarUrl),
            imageView = imageAvatar,
            imageType = ImageType.USER,
            injectFilePath = null,
        )

        if (comment.iLike) {
            handleLikeCommentView(
                textLikeComment,
                R.color.inline_action_filled,
                R.drawable.star_filled,
                R.string.comment_liked
            )
        } else {
            handleLikeCommentView(
                textLikeComment,
                R.color.menu_more,
                R.drawable.star_empty,
                R.string.like_comment
            )
        }

        textLikeComment.setOnClickListener {
            viewModel.likeComment(comment, site)
            sendLikeCommentEvent(comment.iLike.not())
        }
        cardReply.setOnClickListener { }
        textReply.text = getString(R.string.comment_reply_to_user, comment.authorName)
    }

    /**
     * Handle like comment text based on the liked status
     */
    private fun handleLikeCommentView(textView: TextView, colorId: Int, drawableId: Int, stringId: Int) {
        textView.text = getString(stringId)
        val color = ContextCompat.getColor(textView.context, colorId)
        textView.setTextColor(color)
        ContextCompat.getDrawable(textView.context, drawableId)
            ?.apply { setTint(color) }
            ?.let { textView.setCompoundDrawablesWithIntrinsicBounds(it, null, null, null) }
    }

    protected open fun sendLikeCommentEvent(liked: Boolean) {
        AnalyticsUtils.trackCommentActionWithSiteDetails(
            if (liked) AnalyticsTracker.Stat.COMMENT_LIKED else AnalyticsTracker.Stat.COMMENT_UNLIKED,
            mCommentSource.toAnalyticsCommentActionSource(),
            site
        )
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
