package org.wordpress.android.ui.comments

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.models.Note
import org.wordpress.android.ui.comments.unified.CommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentSource
import org.wordpress.android.ui.engagement.BottomSheetUiState
import org.wordpress.android.ui.reader.tracker.ReaderTracker.Companion.SOURCE_NOTIF_COMMENT_USER_PROFILE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
import java.util.EnumSet

/**
 * Used when called from notification list for a comment notification
 * [CommentDetailFragment] is too big to be reused
 * It'd be better to have multiple fragments for different sources for different purposes
 */
class NotificationCommentDetailFragment : SharedCommentDetailFragment() {
    override val enabledActions: EnumSet<Note.EnabledActions>
        get() = note.enabledCommentActions

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState?.getString(KEY_NOTE_ID) != null) {
            handleNote(savedInstanceState.getString(KEY_NOTE_ID)!!)
        } else {
            handleNote(requireArguments().getString(KEY_NOTE_ID)!!)
        }

        viewModel.fetchComment(site, note.commentId)
    }

    override fun sendLikeCommentEvent(liked: Boolean) {
        super.sendLikeCommentEvent(liked)
        // it should also track the notification liked/unliked event
        AnalyticsTracker.track(
            if (liked) Stat.NOTIFICATION_LIKED else Stat.NOTIFICATION_UNLIKED
        )
    }

    override fun getUserProfileUiState(): BottomSheetUiState.UserProfileUiState {
        val user = mContentMapper.mapToFormattableContentList(note.body.toString())
            .find { FormattableRangeType.fromString(it.type) == FormattableRangeType.USER }

        return BottomSheetUiState.UserProfileUiState(
            userAvatarUrl = note.iconURL,
            blavatarUrl = "",
            userName = user?.text ?: getString(R.string.anonymous),
            userLogin = mComment?.authorEmail.orEmpty(),
            userBio = "",
            siteTitle = user?.meta?.titles?.home ?: getString(R.string.user_profile_untitled_site),
            siteUrl = user?.ranges?.firstOrNull()?.url.orEmpty(),
            siteId = user?.meta?.ids?.site ?: 0L,
            blogPreviewSource = SOURCE_NOTIF_COMMENT_USER_PROFILE
        )
    }

    override fun getCommentIdentifier(): CommentIdentifier =
        CommentIdentifier.NotificationCommentIdentifier(note.id, note.commentId);

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
                mSite = createDummyWordPressComSite(note.siteId.toLong())
            }
            if (mBinding != null) {
                showComment(mBinding!!, mSite!!, mComment, note)
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
