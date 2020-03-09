package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper.SelectedType.Comments
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper.SelectedType.Likes
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper.SelectedType.Views
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper.SelectedType.Visitors
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OverviewMapper
@Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) {
    private val units = listOf(
            R.string.stats_views,
            R.string.stats_visitors,
            R.string.stats_likes,
            R.string.stats_comments
    )

    enum class SelectedType(val value: Int) {
        Views(0),
        Visitors(1),
        Likes(2),
        Comments(3);

        companion object {
            fun valueOf(value: Int): SelectedType? = values().find { it.value == value }
        }
    }

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
                        R.string.stats_overview_content_description,
                        value,
                        resourceProvider.getString(units[selectedPosition]),
                        statsDateFormatter.printGranularDate(selectedItem.period, statsGranularity),
                        unformattedChange ?: ""
                )
        )
    }

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
            Likes -> this.likes
            Comments -> this.comments
            else -> null
        }
    }

    fun buildColumns(
        selectedItem: PeriodData?,
        onColumnSelected: (position: Int) -> Unit,
        selectedPosition: Int
    ): Columns {
        val views = selectedItem?.views ?: 0
        val visitors = selectedItem?.visitors ?: 0
        val likes = selectedItem?.likes ?: 0
        val comments = selectedItem?.comments ?: 0
        return Columns(
                listOf(
                        Column(
                                R.string.stats_views,
                                statsUtils.toFormattedString(views),
                                contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_views,
                                        views
                                )
                        ),
                        Column(
                                R.string.stats_visitors,
                                statsUtils.toFormattedString(visitors),
                                contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_visitors,
                                        visitors
                                )
                        ),
                        Column(
                                R.string.stats_likes,
                                statsUtils.toFormattedString(likes),
                                contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_likes,
                                        likes
                                )
                        ),
                        Column(
                                R.string.stats_comments,
                                statsUtils.toFormattedString(comments),
                                contentDescriptionHelper.buildContentDescription(
                                        R.string.stats_comments,
                                        comments
                                )
                        )
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
        selectedItemPeriod: String
    ): List<BlockListItem> {
        val chartItems = dates.map {
            val value = when (SelectedType.valueOf(selectedType)) {
                Views -> it.views
                Visitors -> it.visitors
                Likes -> it.likes
                Comments -> it.comments
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

        val entryType = when (SelectedType.valueOf(selectedType)) {
            Visitors -> R.string.stats_visitors
            Likes -> R.string.stats_likes
            Comments -> R.string.stats_comments
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
}
