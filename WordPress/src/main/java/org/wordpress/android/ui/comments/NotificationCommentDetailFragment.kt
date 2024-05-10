@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import androidx.core.view.isGone
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

    override fun handleHeaderVisibility() {
        mBinding?.headerView?.isGone = true
    }

    private fun handleNote(noteId: String) {
        val note = NotificationsTable.getNoteById(noteId)
        if (note == null) {
            // this should not happen
            AppLog.e(AppLog.T.NOTIFS, "Note could not be found.")
            ToastUtils.showToast(activity, R.string.error_notification_open)
            requireActivity().finish()
        } else {
            mNote = note
            mSite = mSiteStore.getSiteBySiteId(note.siteId.toLong())
            if (mSite == null) {
                // This should not exist, we should clean that screen so a note without a site/comment can be displayed
                mSite = createDummyWordPressComSite(mNote!!.siteId.toLong())
            }
            if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
                showComment(mBinding!!, mReplyBinding!!, mActionBinding!!, mSite!!, mComment, mNote)
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
