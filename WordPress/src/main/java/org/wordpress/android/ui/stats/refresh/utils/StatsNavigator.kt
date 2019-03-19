package org.wordpress.android.ui.stats.refresh.utils

import android.content.Intent
import android.support.v4.app.FragmentActivity
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.StatsViewType.AUTHORS
import org.wordpress.android.ui.stats.StatsViewType.CLICKS
import org.wordpress.android.ui.stats.StatsViewType.COMMENTS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_MONTHS_AND_YEARS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_RECENT_WEEKS
import org.wordpress.android.ui.stats.StatsViewType.FOLLOWERS
import org.wordpress.android.ui.stats.StatsViewType.GEOVIEWS
import org.wordpress.android.ui.stats.StatsViewType.PUBLICIZE
import org.wordpress.android.ui.stats.StatsViewType.REFERRERS
import org.wordpress.android.ui.stats.StatsViewType.SEARCH_TERMS
import org.wordpress.android.ui.stats.StatsViewType.TAGS_AND_CATEGORIES
import org.wordpress.android.ui.stats.StatsViewType.TOP_POSTS_AND_PAGES
import org.wordpress.android.ui.stats.StatsViewType.VIDEO_PLAYS
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewMonthsAndYearsStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewRecentWeeksStats
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
@Inject constructor(private val siteProvider: StatsSiteProvider) {
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
                        postType = target.postType,
                        postTitle = target.postTitle,
                        postUrl = target.postUrl)
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
            is ViewMonthsAndYearsStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, DETAIL_MONTHS_AND_YEARS)
            }
            is ViewRecentWeeksStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, DETAIL_RECENT_WEEKS)
            }
            is ViewTag -> {
                ActivityLauncher.openStatsUrl(activity, target.link)
            }
            is ViewPublicizeStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, PUBLICIZE)
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
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, GEOVIEWS)
            }
            is ViewVideoPlays -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, VIDEO_PLAYS)
            }
            is ViewSearchTerms -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, SEARCH_TERMS)
            }
            is ViewAuthors -> {
                ActivityLauncher.viewAllGranularStats(activity, target.statsGranularity, AUTHORS)
            }
            is ViewUrl -> {
                WPWebViewActivity.openURL(activity, target.url)
            }
        }
    }
}
