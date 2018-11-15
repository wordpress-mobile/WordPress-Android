package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import javax.inject.Inject
import kotlin.math.roundToInt

class InsightsMostPopularUseCase
@Inject constructor(private val insightsStore: InsightsStore, private val dateUtils: DateUtils) {
    suspend fun loadMostPopularInsights(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem {
        return if (refresh) {
            val response = insightsStore.fetchMostPopularInsights(site, forced)
            val model = response.model
            val error = response.error

            if (error != null) {
                Failed(R.string.stats_insights_all_time_stats, error.message ?: error.type.name)
            } else {
                loadMostPopularInsightsItem(model)
            }
        } else {
            val model = insightsStore.getMostPopularInsights(site)
            loadMostPopularInsightsItem(model)
        }
    }

    private fun loadMostPopularInsightsItem(model: InsightsMostPopularModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_popular))
        if (model == null) {
            items.add(Empty)
        } else {
            items.add(
                    ListItem(
                            dateUtils.getWeekDay(model.highestDayOfWeek),
                            "${model.highestDayPercent.roundToInt()}%",
                            true
                    )
            )
            items.add(
                    ListItem(
                            dateUtils.getHour(model.highestHour),
                            "${model.highestHourPercent.roundToInt()}%",
                            true
                    )
            )
        }
        return ListInsightItem(items)
    }
}
