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
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject

class OverviewMapper
@Inject constructor(private val statsDateFormatter: StatsDateFormatter) {
    private val units = listOf(
            string.stats_views,
            string.stats_visitors,
            string.stats_likes,
            string.stats_comments
    )

    fun buildTitle(selectedItem: PeriodData?, selectedPosition: Int): ValueItem {
        val value = when (selectedPosition) {
            0 -> selectedItem?.views?.toFormattedString(MILLION)
            1 -> selectedItem?.visitors?.toFormattedString(MILLION)
            2 -> selectedItem?.likes?.toFormattedString(MILLION)
            3 -> selectedItem?.comments?.toFormattedString(MILLION)
            else -> null
        } ?: "0"
        return ValueItem(value = value, unit = units[selectedPosition])
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
