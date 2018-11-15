package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import javax.inject.Inject

class TodayStatsUseCase
@Inject constructor(private val insightsStore: InsightsStore) : BaseInsightsUseCase(TODAY_STATS) {
    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        val dbModel = insightsStore.getTodayInsights(site)
        return dbModel?.let { loadTodayStatsItem(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem {
        val response = insightsStore.fetchTodayInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(R.string.stats_insights_today_stats, error.message ?: error.type.name)
            model != null -> loadTodayStatsItem(model)
            else -> throw IllegalArgumentException("Unexpected empty body")
        }
    }

    private fun loadTodayStatsItem(model: VisitsModel): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_today_stats))
        val hasViews = model.views > 0
        val hasVisitors = model.visitors > 0
        val hasLikes = model.likes > 0
        val hasComments = model.comments > 0
        if (!hasViews && !hasVisitors && !hasLikes && !hasComments) {
            items.add(Empty)
        } else {
            if (hasViews) {
                items.add(
                        Item(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                R.string.stats_views,
                                model.views.toFormattedString(),
                                showDivider = hasVisitors || hasLikes || hasComments
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        Item(
                                R.drawable.ic_user_grey_dark_24dp,
                                R.string.stats_visitors,
                                model.visitors.toFormattedString(),
                                showDivider = hasLikes || hasComments
                        )
                )
            }
            if (hasLikes) {
                items.add(
                        Item(
                                R.drawable.ic_star_grey_dark_24dp,
                                R.string.stats_likes,
                                model.likes.toFormattedString(),
                                showDivider = hasComments
                        )
                )
            }
            if (hasComments) {
                items.add(
                        Item(
                                R.drawable.ic_comment_grey_dark_24dp,
                                R.string.stats_comments,
                                model.comments.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return ListInsightItem(items)
    }
}
