package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper.TotalStatsType.COMMENTS
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper.TotalStatsType.LIKES
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class TotalStatsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils
) {
    fun buildTotalLikesValue(dates: List<PeriodData>): ValueWithChartItem {
        val currentWeekLikes = getCurrentSevenDays(dates, LIKES)
        val currentWeekSumFormatted = sum(currentWeekLikes)

        val previousWeekLikes = getPreviousSevenDays(dates, LIKES)
        val positive = currentWeekLikes.sum() >= previousWeekLikes.sum()

        return ValueWithChartItem(
            value = currentWeekSumFormatted,
            chartValues = currentWeekLikes,
            positive = positive,
            // Add extra bottom margin if there is no information text.
            extraBottomMargin = !shouldShowTotalInformation(currentWeekLikes.sum(), previousWeekLikes.sum())
        )
    }

    fun buildTotalCommentsValue(dates: List<PeriodData>): ValueWithChartItem {
        val currentWeekComments = getCurrentSevenDays(dates, COMMENTS)
        val currentWeekSumFormatted = sum(currentWeekComments)

        val previousWeekComments = getPreviousSevenDays(dates, LIKES)
        val positive = currentWeekComments.sum() >= previousWeekComments.sum()

        return ValueWithChartItem(
            value = currentWeekSumFormatted,
            chartValues = currentWeekComments,
            positive = positive,
            // Add extra bottom margin if there is no information text.
            extraBottomMargin = !shouldShowTotalInformation(currentWeekComments.sum(), previousWeekComments.sum())
        )
    }

    private fun shouldShowTotalInformation(currentWeekSum: Long, previousWeekSum: Long) =
        currentWeekSum != 0L || previousWeekSum != 0L

    fun shouldShowCommentsGuideCard(dates: List<PeriodData>): Boolean {
        return getCurrentSevenDays(dates, COMMENTS).sum() > 0
    }

    fun shouldShowFollowersGuideCard(domainModel: Int): Boolean {
        return domainModel <= 1
    }

    fun shouldShowLikesGuideCard(dates: List<PeriodData>): Boolean {
        return getCurrentSevenDays(dates, LIKES).sum() > 0
    }

    fun buildTotalLikesInformation(dates: List<PeriodData>) = buildTotalInformation(dates, LIKES)

    fun buildTotalCommentsInformation(dates: List<PeriodData>) = buildTotalInformation(dates, COMMENTS)

    private fun buildTotalInformation(dates: List<PeriodData>, type: TotalStatsType): Text? {
        val value = getCurrentSevenDays(dates, type).sum()
        val previousValue = getPreviousSevenDays(dates, type).sum()
        if (!shouldShowTotalInformation(value, previousValue)) {
            return null
        }
        val positive = value >= previousValue
        val change = statsUtils.buildChange(previousValue, value, positive, true).toString()
        val stringRes = if (positive) {
            R.string.stats_insights_total_stats_seven_days_positive
        } else {
            R.string.stats_insights_total_stats_seven_days_negative
        }

        return Text(
            text = resourceProvider.getString(stringRes, change),
            color = when {
                positive -> mapOf(R.color.stats_color_positive to change)
                else -> mapOf(R.color.stats_color_negative to change)
            }
        )
    }

    private fun sum(list: List<Long>): String {
        val sum = list.sum()
        return statsUtils.toFormattedString(sum, MILLION)
    }

    /**
     * Gives list of data with StatsType for the current week.
     */
    fun getCurrentSevenDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        // taking data for last 14 days excluding today
        AppLog.d(AppLog.T.STATS, dates.toString())
        return mapToStatsType(dates.dropLast(1).takeLast(DAY_COUNT_FOR_CURRENT_WEEK), type)
    }

    /**
     * Gives list of data with StatsType for previous week.
     */
    fun getPreviousSevenDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        return if (dates.size < DAY_COUNT_TOTAL) {
            emptyList()
        } else {
            mapToStatsType(dates.take(DAY_COUNT_FOR_PREVIOUS_WEEK), type)
        }
    }

    /**
     * Gives list of data with StatsType for the current week.
     */
    fun getCurrentWeekDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        // taking data for last 14 days excluding today
        AppLog.d(AppLog.T.STATS, dates.toString())
        return mapToStatsType(dates.takeLast(DAY_COUNT_FOR_CURRENT_WEEK), type)
    }

    /**
     * Gives list of data with StatsType for previous week.
     */
    fun getPreviousWeekDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        return if (dates.size < DAY_COUNT_TOTAL) {
            emptyList()
        } else {
            mapToStatsType(dates.drop(1).take(DAY_COUNT_FOR_PREVIOUS_WEEK), type)
        }
    }

    private fun mapToStatsType(dates: List<PeriodData>, type: TotalStatsType) = dates.map {
        when (type) {
            TotalStatsType.VIEWS -> it.views
            TotalStatsType.VISITORS -> it.visitors
            LIKES -> it.likes
            COMMENTS -> it.comments
        }
    }

    enum class TotalStatsType { VIEWS, VISITORS, LIKES, COMMENTS }

    companion object {
        private const val DAY_COUNT_FOR_CURRENT_WEEK = 7 // Last 7 days
        private const val DAY_COUNT_FOR_PREVIOUS_WEEK = 7 // Last 7 days before the current week
        private const val DAY_COUNT_TOTAL = DAY_COUNT_FOR_PREVIOUS_WEEK + DAY_COUNT_FOR_CURRENT_WEEK
    }
}
