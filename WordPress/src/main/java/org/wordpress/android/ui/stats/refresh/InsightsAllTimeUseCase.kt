package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import javax.inject.Inject

class InsightsAllTimeUseCase
@Inject constructor(private val insightsStore: InsightsStore) : BaseInsightsUseCase(ALL_TIME_STATS) {
    override suspend fun fetch(
        site: SiteModel,
        refresh: Boolean,
        forced: Boolean
    ) {
        if (liveData.value == null) {
            val dbModel = insightsStore.getAllTimeInsights(site)
            if (dbModel != null) {
                mutableLiveData.postValue(loadAllTimeInsightsItem(dbModel))
            } else {
                mutableLiveData.postValue(ListInsightItem(listOf(Empty)))
            }
        }
        if (refresh) {
            val response = insightsStore.fetchAllTimeInsights(site, forced)
            val model = response.model
            val error = response.error
            mutableLiveData.postValue(
                    when {
                        model != null -> loadAllTimeInsightsItem(model)
                        error != null -> Failed(
                                R.string.stats_insights_all_time_stats,
                                error.message ?: error.type.name
                        )
                        else -> throw Exception("All times stats response should contain a model or an error")
                    }
            )
        }
    }

    private fun loadAllTimeInsightsItem(model: InsightsAllTimeModel): ListInsightItem {
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
                                R.string.posts,
                                model.posts.toFormattedString(),
                                showDivider = hasViews || hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasViews) {
                items.add(
                        Item(
                                R.drawable.ic_visible_on_grey_dark_24dp,
                                R.string.stats_views,
                                model.views.toFormattedString(),
                                showDivider = hasVisitors || hasViewsBestDayTotal
                        )
                )
            }
            if (hasVisitors) {
                items.add(
                        Item(
                                R.drawable.ic_user_grey_dark_24dp,
                                R.string.stats_visitors,
                                model.visitors.toFormattedString(),
                                showDivider = hasViewsBestDayTotal
                        )
                )
            }
            if (hasViewsBestDayTotal) {
                items.add(
                        Item(
                                R.drawable.ic_trophy_grey_dark_24dp,
                                R.string.stats_insights_best_ever,
                                model.viewsBestDayTotal.toFormattedString(),
                                showDivider = false
                        )
                )
            }
        }
        return ListInsightItem(items)
    }
}
