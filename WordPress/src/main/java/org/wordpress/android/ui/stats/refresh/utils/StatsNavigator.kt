package org.wordpress.android.ui.stats.refresh.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_STATS
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.stats.StatsViewType.ANNUAL_STATS
import org.wordpress.android.ui.stats.StatsViewType.AUTHORS
import org.wordpress.android.ui.stats.StatsViewType.CLICKS
import org.wordpress.android.ui.stats.StatsViewType.COMMENTS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_MONTHS_AND_YEARS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_RECENT_WEEKS
import org.wordpress.android.ui.stats.StatsViewType.EMAILS
import org.wordpress.android.ui.stats.StatsViewType.FILE_DOWNLOADS
import org.wordpress.android.ui.stats.StatsViewType.FOLLOWERS
import org.wordpress.android.ui.stats.StatsViewType.GEOVIEWS
import org.wordpress.android.ui.stats.StatsViewType.PUBLICIZE
import org.wordpress.android.ui.stats.StatsViewType.REFERRERS
import org.wordpress.android.ui.stats.StatsViewType.SEARCH_TERMS
import org.wordpress.android.ui.stats.StatsViewType.SUBSCRIBERS
import org.wordpress.android.ui.stats.StatsViewType.TAGS_AND_CATEGORIES
import org.wordpress.android.ui.stats.StatsViewType.TOP_POSTS_AND_PAGES
import org.wordpress.android.ui.stats.StatsViewType.VIDEO_PLAYS
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsNavigator @Inject constructor(
    private val siteProvider: StatsSiteProvider,
    private val selectedDateProvider: SelectedDateProvider,
    private val readerTracker: ReaderTracker
) {
    @Suppress("LongMethod", "ComplexMethod", "SwallowedException")
    fun navigate(activity: FragmentActivity, target: NavigationTarget) {
        when (target) {
            is NavigationTarget.AddNewPost -> {
                ActivityLauncher.addNewPostForResult(activity, siteProvider.siteModel, false, POST_FROM_STATS, -1, null)
            }
            is NavigationTarget.ViewPost -> {
                StatsNavigatorHelper.openPostInReaderOrInAppWebView(
                    activity,
                    siteProvider.siteModel.siteId,
                    target.postId,
                    target.postType,
                    target.postUrl,
                    readerTracker
                )
            }
            is NavigationTarget.SharePost -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, target.url)
                intent.putExtra(Intent.EXTRA_SUBJECT, target.title)
                try {
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_link)))
                } catch (ex: ActivityNotFoundException) {
                    ToastUtils.showToast(activity, R.string.reader_toast_err_share_intent)
                }
            }
            is NavigationTarget.ViewPostDetailStats -> {
                StatsDetailActivity.start(
                    activity,
                    siteProvider.siteModel,
                    target.postId,
                    postType = target.postType,
                    postTitle = target.postTitle,
                    postUrl = target.postUrl
                )
            }
            is NavigationTarget.ViewFollowersStats -> {
                ActivityLauncher.viewAllTabbedInsightsStats(
                    activity,
                    FOLLOWERS,
                    target.selectedTab,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewCommentsStats -> {
                ActivityLauncher.viewAllTabbedInsightsStats(
                    activity,
                    COMMENTS,
                    target.selectedTab,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewTagsAndCategoriesStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, TAGS_AND_CATEGORIES, siteProvider.siteModel.id)
            }
            is NavigationTarget.ViewMonthsAndYearsStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, DETAIL_MONTHS_AND_YEARS, siteProvider.siteModel.id)
            }
            is NavigationTarget.ViewRecentWeeksStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, DETAIL_RECENT_WEEKS, siteProvider.siteModel.id)
            }
            is NavigationTarget.ViewTag -> {
                ActivityLauncher.openStatsUrl(activity, target.link)
            }
            is NavigationTarget.ViewPublicizeStats -> {
                ActivityLauncher.viewAllInsightsStats(activity, PUBLICIZE, siteProvider.siteModel.id)
            }
            is NavigationTarget.ViewPostsAndPages -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    TOP_POSTS_AND_PAGES,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewReferrers -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    REFERRERS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewClicks -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    CLICKS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewCountries -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    GEOVIEWS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewVideoPlays -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    VIDEO_PLAYS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewSearchTerms -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    SEARCH_TERMS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewAuthors -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    AUTHORS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewFileDownloads -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    target.statsGranularity,
                    selectedDateProvider.getSelectedDateState(target.statsGranularity),
                    FILE_DOWNLOADS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewAnnualStats -> {
                ActivityLauncher.viewAllGranularStats(
                    activity,
                    YEARS,
                    selectedDateProvider.getSelectedDateState(YEARS),
                    ANNUAL_STATS,
                    siteProvider.siteModel.id
                )
            }
            is NavigationTarget.ViewUrl -> {
                WPWebViewActivity.openURL(activity, target.url)
            }
            is NavigationTarget.ViewInsightsManagement -> {
                ActivityLauncher.viewInsightsManagement(activity, siteProvider.siteModel.id)
            }
            is NavigationTarget.ViewAttachment -> {
                StatsNavigatorHelper.openPostInReaderOrInAppWebView(
                    activity,
                    siteProvider.siteModel.siteId,
                    target.postId,
                    target.postType,
                    target.postUrl,
                    readerTracker
                )
            }
            is NavigationTarget.ViewInsightDetails -> {
                ActivityLauncher.viewInsightsDetail(
                    activity,
                    target.statsSection,
                    target.statsViewType,
                    target.statsGranularity,
                    target.statsGranularity?.let {
                        selectedDateProvider.getSelectedDateState(target.statsGranularity)
                    },
                    siteProvider.siteModel.id
                )
            }

            is NavigationTarget.SubscribersStats -> ActivityLauncher.viewAllInsightsStats(
                activity,
                SUBSCRIBERS,
                siteProvider.siteModel.id
            )

            is NavigationTarget.EmailsStats -> ActivityLauncher.viewAllInsightsStats(
                activity,
                EMAILS,
                siteProvider.siteModel.id
            )
            is NavigationTarget.SetBloggingReminders -> {
                ActivityLauncher.showSetBloggingReminders(activity, siteProvider.siteModel)
            }
            is NavigationTarget.CheckCourse -> {
                ActivityLauncher.openStatsUrl(
                    activity,
                    "https://wpcourses.com/course/intro-to-blogging/"
                )
            }
            is NavigationTarget.SchedulePost -> {
                ActivityLauncher.showSchedulingPost(activity, siteProvider.siteModel)
            }
            NavigationTarget.ViewDayAverageStats -> Unit // Do nothing
        }
    }
}
