@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.notifications

import android.text.TextUtils
import android.view.View
import androidx.fragment.app.Fragment
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.models.Note
import org.wordpress.android.ui.comments.CommentDetailFragment
import org.wordpress.android.ui.comments.unified.CommentActionPopupHandler
import org.wordpress.android.ui.notifications.blocks.NoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.utils.ReaderUtils

class NoteBlockTextClickListener(
    val fragment: Fragment,
    val notification: Note?,
    private val onActionClickListener: CommentDetailFragment.OnActionClickListener? = null
) : NoteBlock.OnNoteBlockTextClickListener {
    override fun onNoteBlockTextClicked(clickedSpan: NoteBlockClickableSpan?) {
        if (!fragment.isAdded || fragment.activity !is NotificationsDetailActivity) {
            return
        }
        clickedSpan?.let { handleNoteBlockSpanClick(fragment.activity as NotificationsDetailActivity, it) }
    }

    override fun showDetailForNoteIds() {
        if (!fragment.isAdded || notification == null || fragment.activity !is NotificationsDetailActivity) {
            return
        }
        val detailActivity = fragment.activity as NotificationsDetailActivity

        requireNotNull(notification).let { note ->
            if (note.isCommentReplyType || !note.isCommentType && note.commentId > 0) {
                val commentId = if (note.isCommentReplyType) note.parentCommentId else note.commentId

                // show comments list if it exists in the reader
                if (ReaderUtils.postAndCommentExists(note.siteId.toLong(), note.postId.toLong(), commentId)) {
                    detailActivity.showReaderCommentsList(note.siteId.toLong(), note.postId.toLong(), commentId)
                } else {
                    detailActivity.showWebViewActivityForUrl(note.url)
                }
            } else if (note.isFollowType) {
                detailActivity.showBlogPreviewActivity(note.siteId.toLong(), note.isFollowType)
            } else {
                // otherwise, load the post in the Reader
                detailActivity.showPostActivity(note.siteId.toLong(), note.postId.toLong())
            }
        }
    }

    override fun showReaderPostComments() {
        if (!fragment.isAdded || notification == null || notification.commentId == 0L) {
            return
        }

        requireNotNull(notification).let { note ->
            fragment.context?.let { nonNullContext ->
                ReaderActivityLauncher.showReaderComments(
                    nonNullContext, note.siteId.toLong(), note.postId.toLong(),
                    note.commentId,
                    ThreadedCommentsActionSource.COMMENT_NOTIFICATION.sourceDescription
                )
            }
        }
    }

    override fun showSitePreview(siteId: Long, siteUrl: String?) {
        if (!fragment.isAdded || notification == null || fragment.activity !is NotificationsDetailActivity) {
            return
        }
        val detailActivity = fragment.activity as NotificationsDetailActivity
        if (siteId != 0L) {
            detailActivity.showBlogPreviewActivity(siteId, notification.isFollowType)
        } else if (!TextUtils.isEmpty(siteUrl)) {
            detailActivity.showWebViewActivityForUrl(siteUrl)
        }
    }

    override fun showActionPopup(view: View) {
        CommentActionPopupHandler.show(view, onActionClickListener)
    }

    fun handleNoteBlockSpanClick(
        activity: NotificationsDetailActivity,
        clickedSpan: NoteBlockClickableSpan
    ) {
        when (clickedSpan.rangeType) {
            FormattableRangeType.SITE ->
                // Show blog preview
                activity.showBlogPreviewActivity(clickedSpan.id, notification?.isFollowType)

            FormattableRangeType.USER ->
                // Show blog preview
                activity.showBlogPreviewActivity(clickedSpan.siteId, notification?.isFollowType)

            FormattableRangeType.POST ->
                // Show post detail
                activity.showPostActivity(clickedSpan.siteId, clickedSpan.id)

            FormattableRangeType.COMMENT ->
                // Load the comment in the reader list if it exists, otherwise show a webview
                if (ReaderUtils.postAndCommentExists(
                        clickedSpan.siteId, clickedSpan.postId,
                        clickedSpan.id
                    )
                ) {
                    activity.showReaderCommentsList(
                        clickedSpan.siteId, clickedSpan.postId,
                        clickedSpan.id
                    )
                } else {
                    activity.showWebViewActivityForUrl(clickedSpan.url)
                }

            FormattableRangeType.SCAN -> activity.showScanActivityForSite(clickedSpan.siteId)
            FormattableRangeType.STAT, FormattableRangeType.FOLLOW ->
                // We can open native stats if the site is a wpcom or Jetpack sites
                activity.showStatsActivityForSite(clickedSpan.siteId, clickedSpan.rangeType)

            FormattableRangeType.LIKE -> if (ReaderPostTable.postExists(clickedSpan.siteId, clickedSpan.id)) {
                activity.showReaderPostLikeUsers(clickedSpan.siteId, clickedSpan.id)
            } else {
                activity.showPostActivity(clickedSpan.siteId, clickedSpan.id)
            }

            FormattableRangeType.REWIND_DOWNLOAD_READY -> activity.showBackupForSite(clickedSpan.siteId)
            else ->
                // We don't know what type of id this is, let's see if it has a URL and push a webview
                if (!TextUtils.isEmpty(clickedSpan.url)) {
                    activity.showWebViewActivityForUrl(clickedSpan.url)
                }
        }
    }
}
