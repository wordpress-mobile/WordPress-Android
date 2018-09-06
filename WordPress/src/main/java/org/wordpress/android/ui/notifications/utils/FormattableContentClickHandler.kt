package org.wordpress.android.ui.notifications.utils

import android.content.Intent
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsViewAllActivity
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class FormattableContentClickHandler
@Inject constructor(val siteStore: SiteStore) {
    private val domainWpCom = "wordpress.com"
    fun onClick(activity: FragmentActivity, clickedSpan: FormattableRange) {
        if (activity.isFinishing) {
            return
        }
        val id = clickedSpan.id ?: 0
        val siteId = clickedSpan.siteId ?: 0
        val rangeType = clickedSpan.rangeType()
        when (rangeType) {
            FormattableRangeType.SITE ->
                // Show blog preview
                showBlogPreviewActivity(activity, id)
            FormattableRangeType.USER ->
                // Show blog preview
                showBlogPreviewActivity(activity, siteId)
            FormattableRangeType.POST ->
                // Show post detail
                showPostActivity(activity, siteId, id)
            FormattableRangeType.COMMENT ->
                // Load the comment in the reader list if it exists, otherwise show a webview
            {
                val postId = clickedSpan.postId ?: 0
                if (ReaderUtils.postAndCommentExists(siteId, postId,
                                id)) {
                    showReaderCommentsList(activity, siteId, postId,
                            id)
                } else {
                    showWebViewActivityForUrl(activity, clickedSpan.url)
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
            else ->
                // We don't know what type of id this is, let's see if it has a URL and push a webview
                if (!TextUtils.isEmpty(clickedSpan.url)) {
                    showWebViewActivityForUrl(activity, clickedSpan.url)
                }
        }
    }

    private fun showBlogPreviewActivity(activity: FragmentActivity, siteId: Long) {
        ReaderActivityLauncher.showReaderBlogPreview(activity, siteId)
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
            val intent = Intent(activity, StatsViewAllActivity::class.java)
            intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, StatsViewType.FOLLOWERS)
            intent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, StatsTimeframe.DAY)
            intent.putExtra(StatsAbstractFragment.ARGS_SELECTED_DATE, "")
            intent.putExtra(StatsAbstractFragment.ARGS_IS_SINGLE_VIEW, true)
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_SITE_ID, site.id)

            intent.putExtra(
                    StatsViewAllActivity.ARG_STATS_VIEW_ALL_TITLE,
                    activity.getString(R.string.stats_view_followers)
            )
            activity.startActivity(intent)
        } else {
            ActivityLauncher.viewBlogStats(activity, site)
        }
    }

    private fun showWebViewActivityForUrl(activity: FragmentActivity, url: String?) {
        if (url == null) {
            return
        }

        if (url.contains(domainWpCom)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(activity, url)
        } else {
            WPWebViewActivity.openURL(activity, url)
        }
    }

    private fun showReaderPostLikeUsers(activity: FragmentActivity, blogId: Long, postId: Long) {
        ReaderActivityLauncher.showReaderLikingUsers(activity, blogId, postId)
    }

    private fun showReaderCommentsList(activity: FragmentActivity, siteId: Long, postId: Long, commentId: Long) {
        ReaderActivityLauncher.showReaderComments(activity, siteId, postId, commentId)
    }
}
