package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.HUNDRED_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OverviewMapper
@Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val resourceProvider: ResourceProvider
) {
    private val units = listOf(
            string.stats_views,
            string.stats_visitors,
            string.stats_likes,
            string.stats_comments
    )

    fun buildTitle(selectedItem: PeriodData?, previousItem: PeriodData?, selectedPosition: Int): ValueItem {
        val value = selectedItem?.getValue(selectedPosition) ?: 0
        val previousValue = previousItem?.getValue(selectedPosition)
        val positive = value >= (previousValue ?: 0)
        val change = previousValue?.let {
            val difference = value - previousValue
            val percentage = when (previousValue) {
                value -> "0"
                0L -> "∞"
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
                unit = units[selectedPosition],
                isFirst = true,
                change = change,
                positive = positive
        )
    }

    private fun PeriodData.getValue(
        selectedPosition: Int
    ): Long? {
        return when (selectedPosition) {
            0 -> this.views
            1 -> this.visitors
            2 -> this.likes
            3 -> this.comments
            else -> null
        }
    }

    fun buildColumns(
        selectedItem: PeriodData?,
        onColumnSelected: (position: Int) -> Unit,
        selectedPosition: Int
    ): Columns {
        return Columns(
                units,
                listOf(
                        selectedItem?.views?.toFormattedString() ?: "0",
                        selectedItem?.visitors?.toFormattedString() ?: "0",
                        selectedItem?.likes?.toFormattedString() ?: "0",
                        selectedItem?.comments?.toFormattedString() ?: "0"
                ),
                selectedPosition,
                onColumnSelected
        )
    }

    fun buildChart(
        dates: List<PeriodData>,
        statsGranularity: StatsGranularity,
        onBarSelected: (String?) -> Unit,
        onBarChartDrawn: (visibleBarCount: Int) -> Unit,
        selectedType: Int,
        selectedPosition: Int
    ): List<BlockListItem> {
        val chartItems = dates.map {
            val value = when (selectedType) {
                0 -> it.views
                1 -> it.visitors
                2 -> it.likes
                3 -> it.comments
                else -> 0L
            }
            Bar(
                    statsDateFormatter.printGranularDate(it.period, statsGranularity),
                    it.period,
                    value.toInt()
            )
        }
        // Only show overlapping visitors when we are showing views
        val shouldShowVisitors = selectedType == 0
        val overlappingItems = if (shouldShowVisitors) {
            dates.map {
                Bar(
                        statsDateFormatter.printGranularDate(it.period, statsGranularity),
                        it.period,
                        it.visitors.toInt()
                )
            }
        } else {
            null
        }
        val result = mutableListOf<BlockListItem>()
        if (shouldShowVisitors) {
            result.add(ChartLegend(R.string.stats_visitors))
        }
        result.add(BarChartItem(
                chartItems,
                overlappingEntries = overlappingItems,
                selectedItem = dates[selectedPosition].period,
                onBarSelected = onBarSelected,
                onBarChartDrawn = onBarChartDrawn
        ))
        return result
    }
}
