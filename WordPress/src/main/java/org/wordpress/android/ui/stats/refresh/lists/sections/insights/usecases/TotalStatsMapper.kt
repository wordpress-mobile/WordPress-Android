package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper.TotalStatsType.COMMENTS
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalStatsMapper.TotalStatsType.LIKES
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@Suppress("TooManyFunctions")
class TotalStatsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils
) {
    fun buildTotalLikesValue(dates: List<PeriodData>): ValueWithChartItem {
        val currentWeekLikes = getCurrentWeekDays(dates, LIKES)
        val currentWeekSumFormatted = sum(currentWeekLikes)

        val previousWeekLikes = getPreviousWeekDays(dates, LIKES)
        val positive = currentWeekLikes.sum() >= previousWeekLikes.sum()

        return ValueWithChartItem(currentWeekSumFormatted, currentWeekLikes, positive)
    }

    fun buildTotalCommentsValue(dates: List<PeriodData>): ValueWithChartItem {
        val currentWeekComments = getCurrentWeekDays(dates, COMMENTS)
        val currentWeekSumFormatted = sum(currentWeekComments)

        val previousWeekComments = getPreviousWeekDays(dates, LIKES)
        val positive = currentWeekComments.sum() >= previousWeekComments.sum()

        return ValueWithChartItem(currentWeekSumFormatted, currentWeekComments, positive)
    }

    fun shouldShowCommentsGuideCard(dates: List<PeriodData>): Boolean {
        return getCurrentWeekDays(dates, COMMENTS).sum() > 0
    }

    fun shouldShowFollowersGuideCard(domainModel: Int): Boolean {
        return domainModel <= 0
    }

    fun shouldShowLikesGuideCard(dates: List<PeriodData>): Boolean {
        return getCurrentWeekDays(dates, LIKES).sum() > 0
    }

    fun buildTotalLikesInformation(dates: List<PeriodData>) = buildTotalInformation(dates, LIKES)

    fun buildTotalCommentsInformation(dates: List<PeriodData>) = buildTotalInformation(dates, COMMENTS)

    private fun buildTotalInformation(dates: List<PeriodData>, type: TotalStatsType): Text? {
        val value = getCurrentWeekDays(dates, type).sum()
        val previousValue = getPreviousWeekDays(dates, type).sum()
        if (value == 0L && previousValue == 0L) {
            return null
        }
        val positive = value >= previousValue
        val change = statsUtils.buildChange(previousValue, value, positive, true).toString()
        val stringRes = if (positive) {
            R.string.stats_insights_total_stats_positive
        } else {
            R.string.stats_insights_total_stats_negative
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
    private fun getCurrentWeekDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        val currentWeekDays = dates.takeLast(DAY_COUNT_FOR_CURRENT_WEEK)
        return mapToStatsType(currentWeekDays, type)
    }

    /**
     * Gives list of data with StatsType for previous week.
     */
    private fun getPreviousWeekDays(dates: List<PeriodData>, type: TotalStatsType): List<Long> {
        if (dates.size < DAY_COUNT_TOTAL) {
            return emptyList()
        }
        val previousWeekDays = dates.subList(
                dates.lastIndex - DAY_COUNT_TOTAL + 1,
                dates.lastIndex - DAY_COUNT_FOR_PREVIOUS_WEEK
        )
        return mapToStatsType(previousWeekDays, type)
    }

    private fun mapToStatsType(dates: List<PeriodData>, type: TotalStatsType) = dates.map {
        when (type) {
            LIKES -> it.likes
            COMMENTS -> it.comments
        }
    }

    private enum class TotalStatsType { LIKES, COMMENTS }

    companion object {
        private const val DAY_COUNT_FOR_CURRENT_WEEK = 8 // Last 7 days + today
        private const val DAY_COUNT_FOR_PREVIOUS_WEEK = 7 // Last 7 days before the current week
        const val DAY_COUNT_TOTAL = DAY_COUNT_FOR_PREVIOUS_WEEK + DAY_COUNT_FOR_CURRENT_WEEK
    }
}
