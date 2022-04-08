package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegendsBlue
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegendsPurple
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips.Chip
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper.SelectedType.Views
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper.SelectedType.Visitors
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ViewsAndVisitorsMapper
@Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) {
    private val units = listOf(
            R.string.stats_views,
            R.string.stats_visitors
    )

    enum class SelectedType(val value: Int) {
        Views(0),
        Visitors(1);

        companion object {
            fun valueOf(value: Int): SelectedType? = values().find { it.value == value }
        }
    }

    fun buildChartLegendsBlue() = ChartLegendsBlue(
            string.stats_timeframe_this_week,
            string.stats_timeframe_previous_week
        )

    fun buildChartLegendsPurple() = ChartLegendsPurple(
            string.stats_timeframe_this_week,
            string.stats_timeframe_previous_week
        )

    @Suppress("LongParameterList")
    fun buildTitle(
        selectedItem: PeriodData,
        previousItem: PeriodData?,
        selectedPosition: Int,
        isLast: Boolean,
        startValue: Int = MILLION,
        statsGranularity: StatsGranularity = DAYS
    ): ValueItem {
        val value = selectedItem.getValue(selectedPosition) ?: 0
        val previousValue = previousItem?.getValue(selectedPosition)
        val positive = value >= (previousValue ?: 0)
        val change = buildChange(previousValue, value, positive, isFormattedNumber = true)
        val unformattedChange = buildChange(previousValue, value, positive, isFormattedNumber = false)
        val state = when {
            isLast -> State.NEUTRAL
            positive -> State.POSITIVE
            else -> State.NEGATIVE
        }
        return ValueItem(
                value = statsUtils.toFormattedString(value, startValue),
                unit = units[selectedPosition],
                isFirst = true,
                change = change,
                state = state,
                contentDescription = resourceProvider.getString(
                        string.stats_overview_content_description,
                        value,
                        resourceProvider.getString(units[selectedPosition]),
                        statsDateFormatter.printGranularDate(selectedItem.period, statsGranularity),
                        unformattedChange ?: ""
                )
        )
    }

    @Suppress("MagicNumber")
    private fun buildChange(
        previousValue: Long?,
        value: Long,
        positive: Boolean,
        isFormattedNumber: Boolean
    ): String? {
        return previousValue?.let {
            val difference = value - previousValue
            val percentage = when (previousValue) {
                value -> "0"
                0L -> "∞"
                else -> mapLongToString((difference * 100 / previousValue), isFormattedNumber)
            }
            val formattedDifference = mapLongToString(difference, isFormattedNumber)
            if (positive) {
                resourceProvider.getString(R.string.stats_traffic_increase, formattedDifference, percentage)
            } else {
                resourceProvider.getString(R.string.stats_traffic_change, formattedDifference, percentage)
            }
        }
    }

    private fun mapLongToString(value: Long, isFormattedNumber: Boolean): String {
        return when (isFormattedNumber) {
            true -> statsUtils.toFormattedString(value)
            false -> value.toString()
        }
    }

    private fun PeriodData.getValue(
        selectedPosition: Int
    ): Long? {
        return when (SelectedType.valueOf(selectedPosition)) {
            Views -> this.views
            Visitors -> this.visitors
            else -> null
        }
    }

    @Suppress("LongParameterList")
    fun buildChart(
        dates: List<PeriodData>,
        statsGranularity: StatsGranularity,
        onBarSelected: (String?) -> Unit,
        onBarChartDrawn: (visibleBarCount: Int) -> Unit,
        selectedType: Int,
        selectedItemPeriod: String
    ): List<BlockListItem> {
        val chartItems = dates.map {
            val value = when (SelectedType.valueOf(selectedType)) {
                Views -> it.views
                Visitors -> it.visitors
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

        val entryType = when (SelectedType.valueOf(selectedType)) {
            Visitors -> R.string.stats_visitors
            else -> R.string.stats_views
        }

        val overlappingType = if (shouldShowVisitors) {
            R.string.stats_visitors
        } else {
            null
        }

        val contentDescriptions = statsUtils.getBarChartEntryContentDescriptions(
                entryType,
                chartItems,
                overlappingType,
                overlappingItems
        )

        result.add(
                BarChartItem(
                        chartItems,
                        overlappingEntries = overlappingItems,
                        selectedItem = selectedItemPeriod,
                        onBarSelected = onBarSelected,
                        onBarChartDrawn = onBarChartDrawn,
                        entryContentDescriptions = contentDescriptions
                )
        )
        return result
    }

    fun buildInformation(): Text {
        return Text(resourceProvider.getString(R.string.stats_insights_views_and_visitors_message))
    }

    fun buildChips(
        selectedItem: PeriodData?,
        onColumnSelected: (position: Int) -> Unit,
        selectedPosition: Int
    ): Chips {
        val views = selectedItem?.views ?: 0
        val visitors = selectedItem?.visitors ?: 0
        return Chips(
                listOf(
                        Chip(
                                string.stats_views,
                                statsUtils.toFormattedString(views),
                                contentDescriptionHelper.buildContentDescription(
                                        string.stats_views,
                                        views
                                )
                        ),
                        Chip(
                                string.stats_visitors,
                                statsUtils.toFormattedString(visitors),
                                contentDescriptionHelper.buildContentDescription(
                                        string.stats_visitors,
                                        visitors
                                )
                        )
                ),
                selectedPosition,
                onColumnSelected
        )
    }
}
