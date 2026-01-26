package org.wordpress.android.ui.newstats.viewsstats

/**
 * UI State for the Views Stats card in the new stats screen.
 */
sealed class ViewsStatsCardUiState {
    data object Loading : ViewsStatsCardUiState()

    data class Loaded(
        val currentPeriodViews: Long,
        val previousPeriodViews: Long,
        val viewsDifference: Long,
        val viewsPercentageChange: Double,
        val currentPeriodDateRange: String,
        val previousPeriodDateRange: String,
        val chartData: ViewsStatsChartData,
        val periodAverage: Long,
        val bottomStats: List<StatItem>,
        val chartType: ChartType = ChartType.LINE
    ) : ViewsStatsCardUiState()

    data class Error(val message: String) : ViewsStatsCardUiState()
}

/**
 * Supported chart types for the views stats chart.
 */
enum class ChartType {
    LINE,
    BAR
}

/**
 * Chart data containing current and previous period views.
 */
data class ViewsStatsChartData(
    val currentPeriod: List<ChartDataPoint>,
    val previousPeriod: List<ChartDataPoint>
)

/**
 * A single data point for the chart.
 * @param label The formatted label for this time unit (e.g., "14:00", "Jan 15", "Jan")
 * @param views The number of views for this time unit
 */
data class ChartDataPoint(
    val label: String,
    val views: Long
)

/**
 * A stat item for the bottom stats row.
 * @param label The display label (e.g., "Views", "Visitors")
 * @param value The stat value
 * @param change The change compared to previous period
 */
data class StatItem(
    val label: String,
    val value: Long,
    val change: StatChange
)

/**
 * Represents the change in a stat compared to the previous period.
 */
sealed class StatChange {
    data class Positive(val percentage: Double) : StatChange()
    data class Negative(val percentage: Double) : StatChange()
    data object NoChange : StatChange()
}
