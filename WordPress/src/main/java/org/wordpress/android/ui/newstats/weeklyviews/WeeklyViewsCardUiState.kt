package org.wordpress.android.ui.newstats.weeklyviews

/**
 * UI State for the Weekly Views card in the new stats screen.
 */
sealed class WeeklyViewsCardUiState {
    data object Loading : WeeklyViewsCardUiState()

    data class Loaded(
        val currentWeekViews: Long,
        val previousWeekViews: Long,
        val viewsDifference: Long,
        val viewsPercentageChange: Double,
        val currentWeekDateRange: String,
        val previousWeekDateRange: String,
        val chartData: WeeklyChartData,
        val weeklyAverage: Long,
        val bottomStats: List<StatItem>
    ) : WeeklyViewsCardUiState()

    data class Error(
        val message: String,
        val onRetry: () -> Unit
    ) : WeeklyViewsCardUiState()
}

/**
 * Chart data containing current and previous week daily views.
 */
data class WeeklyChartData(
    val currentWeek: List<DailyDataPoint>,
    val previousWeek: List<DailyDataPoint>
)

/**
 * A single daily data point for the chart.
 * @param label The formatted label for this day (e.g., "Jan 15")
 * @param views The number of views for this day
 */
data class DailyDataPoint(
    val label: String,
    val views: Long
)

/**
 * A stat item for the bottom stats row.
 * @param label The display label (e.g., "Views", "Visitors")
 * @param value The stat value
 * @param change The change compared to previous week
 */
data class StatItem(
    val label: String,
    val value: Long,
    val change: StatChange
)

/**
 * Represents the change in a stat compared to the previous week.
 */
sealed class StatChange {
    data class Positive(val percentage: Double) : StatChange()
    data class Negative(val percentage: Double) : StatChange()
    data object NoChange : StatChange()
}
