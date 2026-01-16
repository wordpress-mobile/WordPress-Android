package org.wordpress.android.ui.newstats.todaysstat

/**
 * UI State for the Today's Stats card in the new stats screen.
 */
sealed class TodaysStatsCardUiState {
    data object Loading : TodaysStatsCardUiState()

    data class Loaded(
        val views: Int,
        val visitors: Int,
        val likes: Int,
        val comments: Int,
        val chartData: ChartData,
        val onCardClick: () -> Unit
    ) : TodaysStatsCardUiState()

    data class Error(
        val message: String,
        val onRetry: () -> Unit
    ) : TodaysStatsCardUiState()
}

/**
 * Data for the sparkline chart showing views over time.
 * Contains both current period and previous period for comparison.
 */
data class ChartData(
    val currentPeriod: List<ViewsDataPoint>,
    val previousPeriod: List<ViewsDataPoint>
)

/**
 * A single data point for the chart.
 * @param label The label for this point (e.g., day name or date)
 * @param views The number of views for this period
 */
data class ViewsDataPoint(
    val label: String,
    val views: Long
)
