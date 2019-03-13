package org.wordpress.android.ui.stats.refresh.utils

import android.content.Intent
import android.support.v4.app.FragmentActivity
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.StatsViewType.CLICKS
import org.wordpress.android.ui.stats.StatsViewType.COMMENTS
import org.wordpress.android.ui.stats.StatsViewType.FOLLOWERS
import org.wordpress.android.ui.stats.StatsViewType.REFERRERS
import org.wordpress.android.ui.stats.StatsViewType.TAGS_AND_CATEGORIES
import org.wordpress.android.ui.stats.StatsViewType.TOP_POSTS_AND_PAGES
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
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsNavigator
@Inject constructor(private val statsDateFormatter: StatsDateFormatter, private val siteProvider: StatsSiteProvider) {
    fun navigate(activity: FragmentActivity, target: NavigationTarget) {
        when (target) {
            is AddNewPost -> ActivityLauncher.addNewPostForResult(activity, siteProvider.siteModel, false)
            is ViewPost -> {
                StatsUtils.openPostInReaderOrInAppWebview(
                        activity,
                        siteProvider.siteModel.siteId,
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
                StatsDetailActivity.start(
                        activity,
                        siteProvider.siteModel,
                        target.postId,
                        target.postTitle,
                        target.postType,
                        target.postUrl)
            }
            is ViewFollowersStats -> {
                ActivityLauncher.viewAllTabbedInsightsStats(activity, FOLLOWERS, target.selectedTab)
            }
            is ViewCommentsStats -> {
                ActivityLauncher.viewAllTabbedInsightsStats(activity, COMMENTS, target.selectedTab)
            }
            is ViewTagsAndCategoriesStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, TAGS_AND_CATEGORIES)
            }
            is ViewTag -> {
                ActivityLauncher.openStatsUrl(activity, target.link)
            }
            is ViewPublicizeStats -> {
                ActivityLauncher.viewPublicizeStats(activity, siteProvider.siteModel)
            }
            is ViewPostsAndPages -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, TOP_POSTS_AND_PAGES)
            }
            is ViewReferrers -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, REFERRERS)
            }
            is ViewClicks -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, CLICKS)
            }
            is ViewCountries -> {
                ActivityLauncher.viewCountriesStats(
                        activity,
                        siteProvider.siteModel,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewVideoPlays -> {
                ActivityLauncher.viewVideoPlays(
                        activity,
                        siteProvider.siteModel,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewSearchTerms -> {
                ActivityLauncher.viewSearchTerms(
                        activity,
                        siteProvider.siteModel,
                        target.statsGranularity.toStatsTimeFrame(),
                        statsDateFormatter.printStatsDate(target.selectedDate)
                )
            }
            is ViewAuthors -> {
                ActivityLauncher.viewAuthorsStats(
                        activity,
                        siteProvider.siteModel,
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

fun StatsGranularity.toStatsTimeFrame(): StatsTimeframe {
    return when (this) {
        DAYS -> StatsTimeframe.DAY
        WEEKS -> StatsTimeframe.WEEK
        MONTHS -> StatsTimeframe.MONTH
        YEARS -> StatsTimeframe.YEAR
    }
}
