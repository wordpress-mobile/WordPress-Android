@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentSource

/**
 * Used when called from comment list
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class SiteCommentDetailFragment : CommentDetailFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComment(requireArguments().getLong(KEY_COMMENT_ID), requireArguments().getInt(KEY_SITE_LOCAL_ID))

        if (savedInstanceState != null) {
            val siteId = savedInstanceState.getInt(KEY_SITE_LOCAL_ID)
            val commentId = savedInstanceState.getLong(KEY_COMMENT_ID)
            setComment(commentId, siteId)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            site: SiteModel,
            commentModel: CommentModel
        ): SiteCommentDetailFragment {
            val fragment = SiteCommentDetailFragment()
            val args = Bundle()
            args.putSerializable(KEY_MODE, CommentSource.SITE_COMMENTS)
            args.putInt(KEY_SITE_LOCAL_ID, site.id)
            args.putLong(KEY_COMMENT_ID, commentModel.remoteCommentId)
            fragment.arguments = args
            return fragment
        }
    }
}
