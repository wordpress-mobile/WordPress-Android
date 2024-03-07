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
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject

class TrafficOverviewMapper @Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) {
    enum class SelectedType(val value: Int) {
        Views(0),
        Visitors(1),
        Likes(2),
        Comments(3);

        companion object {
            fun valueOf(value: Int): SelectedType? = entries.find { it.value == value }
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
        onBarChartDrawn: (visibleBarCount: Int) -> Unit,
        selectedType: Int
    ): List<BlockListItem> {
        val chartItems = dates.map {
            val value = when (SelectedType.valueOf(selectedType)) {
                Views -> it.views
                Visitors -> it.visitors
                Likes -> it.likes
                Comments -> it.comments
                else -> 0L
            }
            BlockListItem.TrafficBarChartItem.Bar(
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

        val contentDescriptions = statsUtils.getTrafficBarChartEntryContentDescriptions(
            entryType,
            chartItems
        )

        result.add(BlockListItem.TrafficBarChartItem(chartItems, onBarChartDrawn, contentDescriptions))
        return result
    }
}
