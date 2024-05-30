package org.wordpress.android.ui.comments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.Note.EnabledActions
import org.wordpress.android.ui.comments.CommentExtension.getAvatarUrl
import org.wordpress.android.ui.comments.unified.CommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentSource
import org.wordpress.android.ui.engagement.BottomSheetUiState
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import java.util.EnumSet

/**
 * Used when called from comment list
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class SiteCommentDetailFragment : SharedCommentDetailFragment() {
    override val enabledActions: EnumSet<EnabledActions>
        get() = EnumSet.allOf(EnabledActions::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            handleComment(savedInstanceState.getLong(KEY_COMMENT_ID), savedInstanceState.getInt(KEY_SITE_LOCAL_ID))
        } else {
            handleComment(requireArguments().getLong(KEY_COMMENT_ID), requireArguments().getInt(KEY_SITE_LOCAL_ID))
        }
        viewModel.fetchComment(site, comment.remoteCommentId)
    }

    override fun getUserProfileUiState(): BottomSheetUiState.UserProfileUiState =
        BottomSheetUiState.UserProfileUiState(
            userAvatarUrl = comment.getAvatarUrl(
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
