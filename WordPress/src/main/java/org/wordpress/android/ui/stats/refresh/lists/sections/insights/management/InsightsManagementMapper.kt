package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
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

private val POSTS_AND_PAGES_INSIGHTS = listOf(LATEST_POST_SUMMARY, POSTING_ACTIVITY, TAGS_AND_CATEGORIES)
private val ACTIVITY_INSIGHTS = mutableListOf(FOLLOWERS, TOTAL_LIKES, TOTAL_COMMENTS, TOTAL_FOLLOWERS, PUBLICIZE)
private val GENERAL_INSIGHTS = mutableListOf(
    ALL_TIME_STATS,
    MOST_POPULAR_DAY_AND_HOUR,
    TODAY_STATS,
    ANNUAL_SITE_STATS
)

class InsightsManagementMapper @Inject constructor(@Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher) {
    suspend fun buildUIModel(addedTypes: Set<InsightType>, onClick: (InsightType) -> Unit) =
        withContext(bgDispatcher) {
            val insightListItems = mutableListOf<InsightListItem>()
            insightListItems += Header(R.string.stats_insights_management_general)
            if (!GENERAL_INSIGHTS.contains(VIEWS_AND_VISITORS)) {
                GENERAL_INSIGHTS.add(0, VIEWS_AND_VISITORS)
            }
            insightListItems += GENERAL_INSIGHTS.map { type ->
                buildInsightModel(type, addedTypes, onClick)
            }
            insightListItems += Header(R.string.stats_insights_management_posts_and_pages)
            insightListItems += POSTS_AND_PAGES_INSIGHTS.map { type ->
                buildInsightModel(type, addedTypes, onClick)
            }
            insightListItems += Header(R.string.stats_insights_management_activity)

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
        FOLLOWERS -> R.string.stats_view_subscribers
        TODAY_STATS -> R.string.stats_insights_today
        POSTING_ACTIVITY -> R.string.stats_insights_posting_activity
        PUBLICIZE -> R.string.stats_view_publicize
        ANNUAL_SITE_STATS -> R.string.stats_insights_this_year_site_stats
        TOTAL_LIKES -> R.string.stats_view_total_likes
        TOTAL_COMMENTS -> R.string.stats_view_total_comments
        TOTAL_FOLLOWERS -> R.string.stats_view_total_subscribers
        AUTHORS_COMMENTS -> R.string.stats_comments_authors
        POSTS_COMMENTS -> R.string.stats_comments_posts_and_pages
        FOLLOWER_TYPES, ACTION_REMINDER, ACTION_SCHEDULE, ACTION_GROW, FOLLOWER_TOTALS -> null
    }
}
