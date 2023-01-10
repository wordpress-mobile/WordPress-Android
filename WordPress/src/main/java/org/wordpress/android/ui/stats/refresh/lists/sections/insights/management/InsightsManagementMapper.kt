package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_GROW
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_REMINDER
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_SCHEDULE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.AUTHORS_COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TYPES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTS_COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_LIKES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status.ADDED
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status.REMOVED
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject
import javax.inject.Named

private val POSTS_AND_PAGES_INSIGHTS = listOf(
    LATEST_POST_SUMMARY, POSTING_ACTIVITY, TAGS_AND_CATEGORIES
)
private val ACTIVITY_INSIGHTS = mutableListOf(
    FOLLOWERS,
    FOLLOWER_TOTALS,
    PUBLICIZE
)
private val GENERAL_INSIGHTS = mutableListOf(
    ALL_TIME_STATS,
    MOST_POPULAR_DAY_AND_HOUR,
    ANNUAL_SITE_STATS,
    TODAY_STATS
)

class InsightsManagementMapper @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun buildUIModel(addedTypes: Set<InsightType>, onClick: (InsightType) -> Unit) =
        withContext(bgDispatcher) {
            val insightListItems = mutableListOf<InsightListItem>()
            insightListItems += Header(string.stats_insights_management_general)
            if (BuildConfig.IS_JETPACK_APP && !GENERAL_INSIGHTS.contains(VIEWS_AND_VISITORS)) {
                GENERAL_INSIGHTS.add(0, VIEWS_AND_VISITORS)
            }
            insightListItems += GENERAL_INSIGHTS.map { type ->
                buildInsightModel(type, addedTypes, onClick)
            }
            insightListItems += Header(string.stats_insights_management_posts_and_pages)
            insightListItems += POSTS_AND_PAGES_INSIGHTS.map { type ->
                buildInsightModel(type, addedTypes, onClick)
            }
            insightListItems += Header(string.stats_insights_management_activity)

            if (BuildConfig.IS_JETPACK_APP && ACTIVITY_INSIGHTS.contains(FOLLOWER_TOTALS)) {
                // Replace FOLLOWER_TOTALS with Stats revamp v2 total insights
                val followerTotalsIndex = ACTIVITY_INSIGHTS.indexOf(FOLLOWER_TOTALS)
                ACTIVITY_INSIGHTS.remove(FOLLOWER_TOTALS)

                val statsRevampV2TotalInsights = listOf(TOTAL_LIKES, TOTAL_COMMENTS, TOTAL_FOLLOWERS)
                ACTIVITY_INSIGHTS.addAll(followerTotalsIndex, statsRevampV2TotalInsights)
            }
            insightListItems += ACTIVITY_INSIGHTS.map { type ->
                buildInsightModel(type, addedTypes, onClick)
            }
            insightListItems
        }

    private fun buildInsightModel(
        type: InsightType,
        addedInsightTypes: Set<InsightType>,
        onClick: (InsightType) -> Unit
    ): InsightModel {
        return InsightModel(
            type,
            toName(type),
            if (addedInsightTypes.any { it == type }) ADDED else REMOVED,
            ListItemInteraction.create(type, onClick)
        )
    }

    @Suppress("ComplexMethod")
    private fun toName(insightType: InsightType) = when (insightType) {
        VIEWS_AND_VISITORS -> R.string.stats_insights_views_and_visitors
        LATEST_POST_SUMMARY -> R.string.stats_insights_latest_post_summary
        MOST_POPULAR_DAY_AND_HOUR -> R.string.stats_insights_popular
        ALL_TIME_STATS -> R.string.stats_insights_all_time_stats
        TAGS_AND_CATEGORIES -> R.string.stats_insights_tags_and_categories
        COMMENTS -> R.string.stats_comments
        FOLLOWERS -> R.string.stats_view_followers
        TODAY_STATS -> R.string.stats_insights_today
        POSTING_ACTIVITY -> R.string.stats_insights_posting_activity
        PUBLICIZE -> R.string.stats_view_publicize
        ANNUAL_SITE_STATS -> R.string.stats_insights_this_year_site_stats
        TOTAL_LIKES -> string.stats_view_total_likes
        TOTAL_COMMENTS -> string.stats_view_total_comments
        TOTAL_FOLLOWERS -> string.stats_view_total_followers
        AUTHORS_COMMENTS -> string.stats_comments_authors
        POSTS_COMMENTS -> string.stats_comments_posts_and_pages
        FOLLOWER_TOTALS -> R.string.stats_view_follower_totals
        FOLLOWER_TYPES -> null
        ACTION_REMINDER, ACTION_SCHEDULE, ACTION_GROW -> null
    }
}
