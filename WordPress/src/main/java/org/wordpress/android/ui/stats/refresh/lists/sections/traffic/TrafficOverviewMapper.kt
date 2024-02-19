package org.wordpress.android.ui.stats.refresh.lists.sections.traffic

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.traffic.TrafficOverviewMapper.SelectedType.Comments
import org.wordpress.android.ui.stats.refresh.lists.sections.traffic.TrafficOverviewMapper.SelectedType.Likes
import org.wordpress.android.ui.stats.refresh.lists.sections.traffic.TrafficOverviewMapper.SelectedType.Views
import org.wordpress.android.ui.stats.refresh.lists.sections.traffic.TrafficOverviewMapper.SelectedType.Visitors
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class TrafficOverviewMapper @Inject constructor(
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
            fun valueOf(value: Int): SelectedType? = entries.find { it.value == value }
        }
    }

    fun buildTitle(
        selectedItem: VisitsAndViewsModel.PeriodData,
        previousItem: VisitsAndViewsModel.PeriodData?,
        selectedPosition: Int,
        isLast: Boolean,
        startValue: Int = MILLION,
        statsGranularity: StatsGranularity = StatsGranularity.DAYS
    ): BlockListItem.ValueItem {
        val value = selectedItem.getValue(selectedPosition) ?: 0
        val previousValue = previousItem?.getValue(selectedPosition)
        val positive = value >= (previousValue ?: 0)
        val change = statsUtils.buildChange(previousValue, value, positive, isFormattedNumber = true)
        val period = when (statsGranularity) {
            StatsGranularity.WEEKS -> R.string.stats_traffic_change_weeks
            StatsGranularity.MONTHS -> R.string.stats_traffic_change_months
            StatsGranularity.YEARS -> R.string.stats_traffic_change_years
            else -> R.string.stats_traffic_change_weeks
        }
        val unformattedChange = statsUtils.buildChange(previousValue, value, positive, isFormattedNumber = false)
        val state = when {
            isLast -> BlockListItem.ValueItem.State.NEUTRAL
            positive -> BlockListItem.ValueItem.State.POSITIVE
            else -> BlockListItem.ValueItem.State.NEGATIVE
        }
        return BlockListItem.ValueItem(
            value = statsUtils.toFormattedString(value, startValue),
            unit = units[selectedPosition],
            isFirst = true,
            change = change,
            period = period,
            state = state,
            contentDescription = resourceProvider.getString(
                R.string.stats_overview_content_description,
                value,
                resourceProvider.getString(units[selectedPosition]),
                "",
                statsDateFormatter.printGranularDate(selectedItem.period, statsGranularity),
                unformattedChange ?: ""
            )
        )
    }

    private fun VisitsAndViewsModel.PeriodData.getValue(
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
        selectedItem: VisitsAndViewsModel.PeriodData?,
        onColumnSelected: (position: Int) -> Unit,
        selectedPosition: Int
    ): BlockListItem.Columns {
        val views = selectedItem?.views ?: 0
        val visitors = selectedItem?.visitors ?: 0
        val likes = selectedItem?.likes ?: 0
        val comments = selectedItem?.comments ?: 0
        return BlockListItem.Columns(
            listOf(
                BlockListItem.Columns.Column(
                    R.string.stats_views,
                    statsUtils.toFormattedString(views),
                    contentDescriptionHelper.buildContentDescription(
                        R.string.stats_views,
                        views
                    )
                ),
                BlockListItem.Columns.Column(
                    R.string.stats_visitors,
                    statsUtils.toFormattedString(visitors),
                    contentDescriptionHelper.buildContentDescription(
                        R.string.stats_visitors,
                        visitors
                    )
                ),
                BlockListItem.Columns.Column(
                    R.string.stats_likes,
                    statsUtils.toFormattedString(likes),
                    contentDescriptionHelper.buildContentDescription(
                        R.string.stats_likes,
                        likes
                    )
                ),
                BlockListItem.Columns.Column(
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

    @Suppress("LongParameterList", "LongMethod")
    fun buildChart(
        dates: List<VisitsAndViewsModel.PeriodData>,
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
            BlockListItem.BarChartItem.Bar(
                statsDateFormatter.printTrafficGranularDate(it.period, statsGranularity),
                it.period,
                value.toInt()
            )
        }

        val result = mutableListOf<BlockListItem>()

        val entryType = when (SelectedType.valueOf(selectedType)) {
            Visitors -> R.string.stats_visitors
            Likes -> R.string.stats_likes
            Comments -> R.string.stats_comments
            else -> R.string.stats_views
        }

        val contentDescriptions = statsUtils.getBarChartEntryContentDescriptions(
            entryType,
            chartItems
        )

        result.add(
            BlockListItem.BarChartItem(
                chartItems,
                selectedItem = selectedItemPeriod,
                onBarSelected = onBarSelected,
                onBarChartDrawn = onBarChartDrawn,
                entryContentDescriptions = contentDescriptions
            )
        )
        return result
    }
}
