@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.os.Bundle
import org.wordpress.android.ui.comments.unified.CommentSource

/**
 * Used when called from notification list for a comment notification
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class NotificationCommentDetailFragment: CommentDetailFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNote(requireArguments().getString(KEY_NOTE_ID))

        if (savedInstanceState?.getString(KEY_NOTE_ID) != null) {
            // The note will be set in onResume()
            // See WordPress.deferredInit()
            mRestoredNoteId = savedInstanceState.getString(KEY_NOTE_ID)
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(noteId: String): CommentDetailFragment {
            val fragment = NotificationCommentDetailFragment()
            val args = Bundle()
            args.putSerializable(KEY_MODE, CommentSource.NOTIFICATION)
            args.putString(KEY_NOTE_ID, noteId)
            fragment.arguments = args
            return fragment
        }
    }
}
