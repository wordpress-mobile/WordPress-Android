package org.wordpress.android.ui.stats.refresh.lists.detail

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State
import org.wordpress.android.ui.stats.refresh.utils.HUNDRED_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PostDayViewsMapper
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val statsDateFormatter: StatsDateFormatter
) {
    fun buildTitle(
        selectedItem: Day,
        previousItem: Day?,
        isLast: Boolean
    ): ValueItem {
        val value = selectedItem.count
        val previousValue = previousItem?.count
        val positive = value >= (previousValue ?: 0)
        val change = buildChange(previousValue, value, positive, isFormattedNumber = true)
        val unformattedChange = buildChange(previousValue, value, positive, isFormattedNumber = false)

        val state = when {
            isLast -> State.NEUTRAL
            positive -> State.POSITIVE
            else -> State.NEGATIVE
        }
        return ValueItem(
                value = statsUtils.toFormattedString(value, HUNDRED_THOUSAND),
                unit = R.string.stats_views,
                isFirst = true,
                change = change,
                state = state,
                contentDescription = resourceProvider.getString(
                        R.string.stats_overview_content_description,
                        value,
                        resourceProvider.getString(R.string.stats_views),
                        statsDateFormatter.printDate(selectedItem.period),
                        unformattedChange ?: ""
                )
        )
    }

    private fun buildChange(
        previousValue: Int?,
        value: Int,
        positive: Boolean,
        isFormattedNumber: Boolean
    ): String? {
        return previousValue?.let {
            val difference = value - previousValue
            val percentage = when (previousValue) {
                value -> "0"
                0 -> "∞"
                else -> mapIntToString((difference * 100 / previousValue), isFormattedNumber)
            }
            val formattedDifference = mapIntToString(difference, isFormattedNumber)
            if (positive) {
                resourceProvider.getString(R.string.stats_traffic_increase, formattedDifference, percentage)
            } else {
                resourceProvider.getString(R.string.stats_traffic_change, formattedDifference, percentage)
            }
        }
    }

    private fun mapIntToString(value: Int, isFormattedNumber: Boolean): String {
        return when (isFormattedNumber) {
            true -> statsUtils.toFormattedString(value)
            false -> value.toString()
        }
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

        val contentDescriptions = statsUtils.getBarChartEntryContentDescriptions(
                R.string.stats_views,
                chartItems
        )

        result.add(
                BarChartItem(
                        chartItems,
                        selectedItem = selectedDay,
                        onBarSelected = onBarSelected,
                        onBarChartDrawn = onBarChartDrawn,
                        entryContentDescriptions = contentDescriptions
                )
        )
        return result
    }
}
