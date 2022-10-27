package org.wordpress.android.ui.notifications.utils

import androidx.fragment.app.FragmentActivity
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.ACTIVITY_LOG_DETAIL
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.stats.StatsViewType.FOLLOWERS
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

private const val DOMAIN_WP_COM = "wordpress.com"

class FormattableContentClickHandler @Inject constructor(
    val siteStore: SiteStore,
    val readerTracker: ReaderTracker
) {
    fun onClick(
        activity: FragmentActivity,
        clickedSpan: FormattableRange,
        source: String
    ) {
        if (activity.isFinishing) {
            return
        }
        val id = clickedSpan.id ?: 0
        val siteId = clickedSpan.siteId ?: 0
        val postId = clickedSpan.postId ?: 0
        when (val rangeType = clickedSpan.rangeType()) {
            FormattableRangeType.SITE ->
                // Show blog preview
                showBlogPreviewActivity(activity, id, postId, source)
            FormattableRangeType.USER ->
                // Show blog preview
                showBlogPreviewActivity(activity, siteId, postId, source)
            FormattableRangeType.PAGE, FormattableRangeType.POST ->
                // Show post detail
                showPostActivity(activity, siteId, id)
            FormattableRangeType.COMMENT -> {
                // Load the comment in the reader list if it exists, otherwise show a webview
                val rootId = clickedSpan.postId ?: clickedSpan.rootId ?: 0
                if (ReaderUtils.postAndCommentExists(siteId, rootId, id)) {
                    showReaderCommentsList(activity, siteId, rootId, id, ACTIVITY_LOG_DETAIL)
                } else {
                    showWebViewActivityForUrl(activity, clickedSpan.url, rangeType)
                }
            }
            FormattableRangeType.STAT, FormattableRangeType.FOLLOW ->
                // We can open native stats if the site is a wpcom or Jetpack sites
                showStatsActivityForSite(activity, siteId, rangeType)
            FormattableRangeType.LIKE -> if (ReaderPostTable.postExists(siteId, id)) {
                showReaderPostLikeUsers(activity, siteId, id)
            } else {
                showPostActivity(activity, siteId, id)
            }
            FormattableRangeType.REWIND_DOWNLOAD_READY -> showBackup(activity, siteId)
            FormattableRangeType.BLOCKQUOTE,
            FormattableRangeType.NOTICON,
            FormattableRangeType.MATCH,
            FormattableRangeType.MEDIA,
            FormattableRangeType.UNKNOWN -> {
                showWebViewActivityForUrl(activity, clickedSpan.url, rangeType)
            }
            FormattableRangeType.SCAN -> Unit // Do nothing
            FormattableRangeType.B -> Unit // Do nothing
        }
    }

    private fun showBlogPreviewActivity(
        activity: FragmentActivity,
        siteId: Long,
        postId: Long,
        source: String
    ) {
        val post: ReaderPost? = ReaderPostTable.getBlogPost(siteId, postId, true)
        ReaderActivityLauncher.showReaderBlogPreview(
                activity,
                siteId,
                post?.isFollowedByCurrentUser,
                source,
                readerTracker
        )
    }

    private fun showPostActivity(activity: FragmentActivity, siteId: Long, postId: Long) {
        ReaderActivityLauncher.showReaderPostDetail(activity, siteId, postId)
    }

    private fun showStatsActivityForSite(activity: FragmentActivity, siteId: Long, rangeType: FormattableRangeType) {
        val site = siteStore.getSiteBySiteId(siteId)
        if (site == null) {
            // One way the site can be null: new site created, receive a notification from this site,
            // but the site list is not yet updated in the app.
            ToastUtils.showToast(activity, R.string.blog_not_found)
            return
        }
        showStatsActivityForSite(activity, site, rangeType)
    }

    private fun showStatsActivityForSite(
        activity: FragmentActivity,
        site: SiteModel,
        rangeType: FormattableRangeType
    ) {
        if (rangeType == FormattableRangeType.FOLLOW) {
            ActivityLauncher.viewAllTabbedInsightsStats(activity, FOLLOWERS, 0, site.id)
        } else {
            ActivityLauncher.viewBlogStats(activity, site)
        }
    }

    private fun showWebViewActivityForUrl(activity: FragmentActivity, url: String?, rangeType: FormattableRangeType) {
        if (url == null || url.isEmpty()) {
            AppLog.e(API, "Trying to open web view activity but the URL is missing for range type $rangeType")
            return
        }

        if (url.contains(DOMAIN_WP_COM)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(activity, url)
        } else {
            WPWebViewActivity.openURL(activity, url)
        }
    }

    private fun showReaderPostLikeUsers(activity: FragmentActivity, blogId: Long, postId: Long) {
        ReaderActivityLauncher.showReaderLikingUsers(activity, blogId, postId)
    }

    private fun showReaderCommentsList(
        activity: FragmentActivity,
        siteId: Long,
        postId: Long,
        commentId: Long,
        source: ThreadedCommentsActionSource
    ) {
        ReaderActivityLauncher.showReaderComments(
                activity,
                siteId,
                postId,
                commentId,
                source.sourceDescription
        )
    }

    private fun showBackup(activity: FragmentActivity, siteId: Long) {
        val site = siteStore.getSiteBySiteId(siteId)
        ActivityLauncher.viewBackupList(activity, site)
    }
}
