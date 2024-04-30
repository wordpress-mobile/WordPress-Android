@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.ui.comments.unified.CommentSource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils

/**
 * Used when called from notification list for a comment notification
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class NotificationCommentDetailFragment : CommentDetailFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.getString(KEY_NOTE_ID) != null) {
            handleNote(savedInstanceState.getString(KEY_NOTE_ID)!!)
        } else {
            handleNote(requireArguments().getString(KEY_NOTE_ID)!!)
        }
    }

    private fun handleNote(noteId: String) {
        note = NotificationsTable.getNoteById(noteId)
        if (note == null) {
            // this should not happen
            AppLog.e(AppLog.T.NOTIFS, "Note could not be found.")
            ToastUtils.showToast(activity, R.string.error_notification_open)
            requireActivity().finish()
        } else {
            site = mSiteStore.getSiteBySiteId(note!!.siteId.toLong())
            if (site == null) {
                // This should not exist, we should clean that screen so a note without a site/comment can be displayed
                site = createDummyWordPressComSite(note!!.siteId.toLong())
            }
            if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
                showComment(mBinding!!, mReplyBinding!!, mActionBinding!!, site!!, mComment, note)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(noteId: String): NotificationCommentDetailFragment {
            val fragment = NotificationCommentDetailFragment()
            val args = Bundle()
            args.putSerializable(KEY_MODE, CommentSource.NOTIFICATION)
            args.putString(KEY_NOTE_ID, noteId)
            fragment.arguments = args
            return fragment
        }
    }
}
