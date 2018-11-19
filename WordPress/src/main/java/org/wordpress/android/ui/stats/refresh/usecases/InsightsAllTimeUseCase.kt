package org.wordpress.android.ui.stats.refresh.usecases

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.Failed
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.ListInsightItem
import org.wordpress.android.ui.stats.refresh.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class InsightsAllTimeUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore
) : BaseInsightsUseCase(ALL_TIME_STATS, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        val dbModel = insightsStore.getAllTimeInsights(site)
        return dbModel?.let { loadAllTimeInsightsItem(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem {
        val response = insightsStore.fetchAllTimeInsights(site, forced)
        val model = response.model
        val error = response.error

        return if (error != null) {
            Failed(R.string.stats_insights_all_time_stats, error.message ?: error.type.name)
        } else {
            loadAllTimeInsightsItem(model)
        }
    }

    private fun loadAllTimeInsightsItem(model: InsightsAllTimeModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_all_time_stats))

        if (model == null) {
            items.add(Empty)
        } else {
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
        }
        return ListInsightItem(items)
    }
}
