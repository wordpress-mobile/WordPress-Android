package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem.Line
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject

class SubscribersMapper @Inject constructor(
    private val statsDateFormatter: StatsDateFormatter,
    private val statsUtils: StatsUtils
) {
    fun buildChart(
        dates: List<PeriodData>,
        onLineSelected: (String?) -> Unit,
        onLineChartDrawn: (visibleBarCount: Int) -> Unit,
        selectedItemPeriod: String
    ): BlockListItem {
        val chartItems = dates.map {
            Line(statsDateFormatter.getStatsDateFromPeriodDay(it.period), it.period, it.subscribers.toInt())
        }

        val contentDescriptions = statsUtils.getSubscribersChartEntryContentDescriptions(
            R.string.stats_subscribers_subscribers,
            chartItems
        )

        return SubscribersChartItem(
            entries = chartItems,
            selectedItemPeriod = selectedItemPeriod,
            onLineSelected = onLineSelected,
            onLineChartDrawn = onLineChartDrawn,
            entryContentDescriptions = contentDescriptions
        )
    }
}
