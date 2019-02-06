package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject

class OverviewMapper
@Inject constructor(private val statsDateFormatter: StatsDateFormatter) {
    fun buildTitle(
        selectedItemPeriod: String?,
        dateFromUiState: String?,
        fallbackDate: String,
        statsGranularity: StatsGranularity
    ): Title {
        val selectedDate = selectedItemPeriod ?: dateFromUiState
        val titleText = if (selectedDate != null) {
            statsDateFormatter.printGranularDate(
                    selectedDate,
                    statsGranularity
            )
        } else {
            statsDateFormatter.printDate(fallbackDate)
        }
        return Title(text = titleText)
    }

    fun buildColumns(
        selectedItem: PeriodData?,
        onColumnSelected: (position: Int) -> Unit,
        selectedPosition: Int
    ): Columns {
        return Columns(
                listOf(
                        string.stats_views,
                        string.stats_visitors,
                        string.stats_likes,
                        string.stats_comments
                ),
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
        domainModel: VisitsAndViewsModel,
        statsGranularity: StatsGranularity,
        onBarSelected: (String?) -> Unit,
        selectedPosition: Int,
        selectedDate: String?
    ): List<BlockListItem> {
        val chartItems = domainModel.dates.map {
            val value = when (selectedPosition) {
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
        val shouldShowVisitors = selectedPosition == 0
        val overlappingItems = if (shouldShowVisitors) {
            domainModel.dates.map {
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
                selectedItem = selectedDate,
                onBarSelected = onBarSelected
        ))
        return result
    }
}
