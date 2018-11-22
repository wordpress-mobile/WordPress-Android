package org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsBlock
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class AllTimeStatsBlock
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : BaseStatsBlock(ALL_TIME_STATS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): StatsListItem? {
        val dbModel = insightsStore.getAllTimeInsights(site)
        return dbModel?.let { loadAllTimeInsightsItem(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): StatsListItem? {
        val response = insightsStore.fetchAllTimeInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> createFailedItem(R.string.stats_insights_all_time_stats, error.message ?: error.type.name)
            else -> model?.let { loadAllTimeInsightsItem(model) }
        }
    }

    private fun loadAllTimeInsightsItem(model: InsightsAllTimeModel): StatsListItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_all_time_stats))

        val hasPosts = model.posts > 0
        val hasViews = model.views > 0
        val hasVisitors = model.visitors > 0
        val hasViewsBestDayTotal = model.viewsBestDayTotal > 0
        if (!hasPosts && !hasViews && !hasVisitors && !hasViewsBestDayTotal) {
            items.add(Empty)
        } else {
            if (hasPosts) {
                items.add(
                        Item(
                                R.drawable.ic_posts_grey_dark_24dp,
                                textResource = R.string.posts,
                                value = model.posts.toFormattedString(),
                                showDivider = hasViews || hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasViews) {
                items.add(
                        Item(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                textResource = R.string.stats_views,
                                value = model.views.toFormattedString(),
                                showDivider = hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        Item(
                                R.drawable.ic_user_grey_dark_24dp,
                                textResource = R.string.stats_visitors,
                                value = model.visitors.toFormattedString(),
                                showDivider = hasViewsBestDayTotal
                        )
                )
            }
            if (hasViewsBestDayTotal) {
                items.add(
                        Item(
                                R.drawable.ic_trophy_grey_dark_24dp,
                                textResource = R.string.stats_insights_best_ever,
                                value = model.viewsBestDayTotal.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return createDataItem(items)
    }
}
