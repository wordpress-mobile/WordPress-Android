package org.wordpress.android.ui.stats.refresh.utils

import android.content.Intent
import android.support.v4.app.FragmentActivity
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.models.StatsPostModel
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewVideoPlays
import org.wordpress.android.ui.stats.refresh.lists.toStatsTimeFrame
import org.wordpress.android.util.ToastUtils

class StatsNavigator(
    private val site: SiteModel,
    private val activity: FragmentActivity,
    private val statsDateFormatter: StatsDateFormatter
) {
    fun navigate(target: NavigationTarget) {
        when (target) {
            is AddNewPost -> ActivityLauncher.addNewPostForResult(activity, site, false)
            is ViewPost -> {
                StatsUtils.openPostInReaderOrInAppWebview(
                        activity,
                        site.siteId,
                        target.postId.toString(),
                        target.postType,
                        target.postUrl
                )
            }
            is SharePost -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, target.url)
                intent.putExtra(Intent.EXTRA_SUBJECT, target.title)
                try {
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_link)))
                } catch (ex: android.content.ActivityNotFoundException) {
                    ToastUtils.showToast(activity, R.string.reader_toast_err_share_intent)
                }
            }
            is ViewPostDetailStats -> {
                val postModel = StatsPostModel(
                        site.siteId,
                        target.postId,
                        target.postTitle,
                        target.postUrl,
                        target.postType
                )
                ActivityLauncher.viewStatsSinglePostDetails(activity, postModel)
            }
            is ViewFollowersStats -> {
                ActivityLauncher.viewFollowersStats(activity, site)
            }
            is ViewCommentsStats -> {
                ActivityLauncher.viewCommentsStats(activity, site)
            }
            is ViewTagsAndCategoriesStats -> {
                ActivityLauncher.viewTagsAndCategoriesStats(activity, site)
            }
            is ViewTag -> {
                ActivityLauncher.openStatsUrl(activity, target.link)
            }
            is ViewPublicizeStats -> {
                ActivityLauncher.viewPublicizeStats(activity, site)
            }
            is ViewPostsAndPages -> {
                ActivityLauncher.viewPostsAndPagesStats(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewReferrers -> {
                ActivityLauncher.viewReferrersStats(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewClicks -> {
                ActivityLauncher.viewClicksStats(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewCountries -> {
                ActivityLauncher.viewCountriesStats(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewVideoPlays -> {
                ActivityLauncher.viewVideoPlays(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewSearchTerms -> {
                ActivityLauncher.viewSearchTerms(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewAuthors -> {
                ActivityLauncher.viewAuthorsStats(
                        activity,
                        site,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewUrl -> {
                WPWebViewActivity.openURL(activity, target.url)
            }
        }
    }
}
