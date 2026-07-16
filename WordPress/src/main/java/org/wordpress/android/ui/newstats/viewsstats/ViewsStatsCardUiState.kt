package org.wordpress.android.ui.newstats.viewsstats

/**
 * UI State for the Views Stats card in the new stats screen.
 *
 * The chart region and the bottom-stats row load from independent calls, so once the card is past
 * the initial [Loading] frame it holds a [Content] whose two sub-states advance on their own. This
 * lets the chart render while the bottom row is still loading (and vice versa), and lets either one
 * fail without hiding the other. [Error] is reserved for fatal cases where nothing can be fetched
 * (no site selected, missing access token).
 */
sealed class ViewsStatsCardUiState {
    data object Loading : ViewsStatsCardUiState()

    data class Content(
        val chart: ChartUiState,
        val bottomStats: BottomStatsUiState,
        val isLoadingNewPeriod: Boolean = false
    ) : ViewsStatsCardUiState()

    data class Error(val message: String) : ViewsStatsCardUiState()
}

/**
 * State of the chart region (header totals + chart). Loads from the two chart calls, independently
 * of the bottom row. [Error] renders a compact retry affordance while the bottom row stays visible.
 */
sealed class ChartUiState {
    data object Loading : ChartUiState()

    data class Loaded(
        val currentPeriodViews: Long,
        val previousPeriodViews: Long,
        val viewsDifference: Long,
        val viewsPercentageChange: Double,
        val currentPeriodDateRange: String,
        val previousPeriodDateRange: String,
        val chartData: ViewsStatsChartData,
        val periodAverage: Long,
        val chartType: ChartType = ChartType.LINE
    ) : ChartUiState()

    data object Error : ChartUiState()
}

/**
 * State of the bottom-stats row. Loads from a dedicated call, independently of the chart. [Hidden]
 * means the dedicated call failed, so the row is dropped rather than shown with missing data.
 */
sealed class BottomStatsUiState {
    data object Loading : BottomStatsUiState()
    data class Loaded(val stats: List<StatItem>) : BottomStatsUiState()
    data object Hidden : BottomStatsUiState()
}

/**
 * Supported chart types for the views stats chart.
 */
enum class ChartType(val storageKey: String) {
    LINE("line"),
    BAR("bar");

    companion object {
        fun fromStorageKey(key: String?): ChartType? =
            entries.firstOrNull { it.storageKey == key }
    }
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
    val views: Long,
    val rawPeriod: String = ""
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
