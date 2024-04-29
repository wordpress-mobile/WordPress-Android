@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentSource

/**
 * Used when called from My Site -> comment list -> comment
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class SiteCommentDetailFragment : CommentDetailFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            handleComment(savedInstanceState.getLong(KEY_COMMENT_ID), savedInstanceState.getInt(KEY_SITE_LOCAL_ID))
        } else {
            handleComment(requireArguments().getLong(KEY_COMMENT_ID), requireArguments().getInt(KEY_SITE_LOCAL_ID))
        }
    }

    private fun handleComment(commentRemoteId: Long, siteLocalId: Int) {
        mSiteStore.getSiteByLocalId(siteLocalId)?.let { site ->
            mSite = site
            val comment = mCommentsStoreAdapter.getCommentBySiteAndRemoteId(site, commentRemoteId)
            mComment = comment
            mIsUsersBlog = comment != null
        }
    }

    override fun onStart() {
        super.onStart()


        // Reset the reply unique id since mComment just changed.
        if (mReplyBinding != null) setReplyUniqueId(mReplyBinding!!, mSite, mComment, mNote)
        showCommentWhenNonNull(mBinding!!, mReplyBinding!!, mActionBinding!!, mSite!!, mComment!!, mNote)
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
