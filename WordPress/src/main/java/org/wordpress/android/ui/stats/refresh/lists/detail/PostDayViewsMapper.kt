package org.wordpress.android.ui.stats.refresh.lists.detail

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.HUNDRED_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PostDayViewsMapper
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val statsDateFormatter: StatsDateFormatter
) {
    fun buildTitle(selectedItem: Day, previousItem: Day?): ValueItem {
        val value = selectedItem.count
        val previousValue = previousItem?.count
        val positive = value >= (previousValue ?: 0)
        val change = previousValue?.let {
            val difference = value - previousValue
            val percentage = when (previousValue) {
                value -> "0"
                0 -> "∞"
                else -> (difference * 100 / previousValue).toFormattedString()
            }
            if (positive) {
                resourceProvider.getString(R.string.stats_traffic_increase, difference.toFormattedString(), percentage)
            } else {
                resourceProvider.getString(R.string.stats_traffic_change, difference.toFormattedString(), percentage)
            }
        }

        return ValueItem(
                value = value.toFormattedString(HUNDRED_THOUSAND),
                unit = R.string.stats_views,
                isFirst = true,
                change = change,
                positive = positive
        )
    }

    fun buildChart(
        dayViews: List<Day>,
        selectedDay: String?,
        onBarSelected: (String?) -> Unit,
        onBarChartDrawn: (visibleBarCount: Int) -> Unit
    ): List<BlockListItem> {
        val chartItems = dayViews.map { day ->
            Bar(
                    statsDateFormatter.printGranularDate(day.period, DAYS),
                    day.period,
                    day.count
            )
        }
        val result = mutableListOf<BlockListItem>()
        result.add(
                BarChartItem(
                        chartItems,
                        selectedItem = selectedDay,
                        onBarSelected = onBarSelected,
                        onBarChartDrawn = onBarChartDrawn
                )
        )
        return result
    }
}
