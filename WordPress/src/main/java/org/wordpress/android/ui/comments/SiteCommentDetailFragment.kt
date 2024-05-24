@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import androidx.core.view.isVisible
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.types.Email
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentSource
import org.wordpress.android.ui.engagement.BottomSheetUiState
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.WPAvatarUtils

/**
 * Used when called from comment list
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class SiteCommentDetailFragment : CommentDetailFragment() {
    private val comment: CommentModel
        get() = mComment!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            handleComment(savedInstanceState.getLong(KEY_COMMENT_ID), savedInstanceState.getInt(KEY_SITE_LOCAL_ID))
        } else {
            handleComment(requireArguments().getLong(KEY_COMMENT_ID), requireArguments().getInt(KEY_SITE_LOCAL_ID))
        }
    }

    override fun getUserProfileUiState(): BottomSheetUiState.UserProfileUiState {
        return BottomSheetUiState.UserProfileUiState(
            userAvatarUrl = CommentExtension.getAvatarUrl(
                comment,
                resources.getDimensionPixelSize(R.dimen.avatar_sz_large)
            ),
            blavatarUrl = "",
            userName = comment.authorName ?: getString(R.string.anonymous),
            userLogin = comment.authorEmail.orEmpty(),
            // keep them empty because there's no data for displaying on UI
            userBio = "",
            siteTitle = "",
            siteUrl = "",
            siteId = 0L,
            blogPreviewSource = ReaderTracker.SOURCE_SITE_COMMENTS_USER_PROFILE
        )
    }

    override fun getCommentIdentifier(): CommentIdentifier =
        CommentIdentifier.SiteCommentIdentifier(comment.id, comment.remoteCommentId)


    override fun handleHeaderVisibility() {
        mBinding?.headerView?.isVisible = true
    }

    private fun handleComment(commentRemoteId: Long, siteLocalId: Int) {
        val site = mSiteStore.getSiteByLocalId(siteLocalId)
        if (site != null) {
            setComment(site, mCommentsStoreAdapter.getCommentBySiteAndRemoteId(site, commentRemoteId))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            site: SiteModel,
            commentModel: CommentModel
        ): SiteCommentDetailFragment = SiteCommentDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_MODE, CommentSource.SITE_COMMENTS)
                putInt(KEY_SITE_LOCAL_ID, site.id)
                putLong(KEY_COMMENT_ID, commentModel.remoteCommentId)
            }
        }
    }
}

object CommentExtension {
    fun getAvatarUrl(comment: CommentModel, size: Int): String = when {
        comment.authorProfileImageUrl != null -> WPAvatarUtils.rewriteAvatarUrl(
            comment.authorProfileImageUrl!!,
            size
        )

        comment.authorEmail != null -> AvatarUrl(
            Email(comment.authorEmail!!),
            AvatarQueryOptions(size, null, null, null)
        ).url().toString()

        else -> ""
    }
}
