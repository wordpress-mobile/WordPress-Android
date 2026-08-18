package org.wordpress.android.ui.newstats.viewsstats

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsColors

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
        val selectedMetric: StatsMetric = StatsMetric.DEFAULT,
        val isLoadingNewPeriod: Boolean = false
    ) : ViewsStatsCardUiState()

    data class Error(val message: String) : ViewsStatsCardUiState()
}

/**
 * State of the chart region (header totals + chart). Loads from the two chart calls, independently
 * of the bottom row. [Error] renders a compact retry affordance while the bottom row stays visible.
 * [Unavailable] means the selected metric has no series for this period (a single-day/hourly response
 * only carries views), so the chart region shows an empty state while the bottom row stays visible.
 */
sealed class ChartUiState {
    data object Loading : ChartUiState()

    // Header totals reflect the currently selected metric, not necessarily views.
    data class Loaded(
        val currentPeriodTotal: Long,
        val previousPeriodTotal: Long,
        val difference: Long,
        val percentageChange: Double,
        val currentPeriodDateRange: String,
        val previousPeriodDateRange: String,
        val chartData: ViewsStatsChartData,
        val periodAverage: Long,
        val chartType: ChartType
    ) : ChartUiState()

    data object Unavailable : ChartUiState()

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
        /**
         * Used when the user has never picked a chart type for the site.
         */
        val DEFAULT = BAR

        fun fromStorageKey(key: String?): ChartType? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * The metrics the Views card can chart and list in the bottom row. Each is a series the non-hourly
 * `stats/visits` response already carries per bucket, so switching between them re-plots the chart
 * without a new network call.
 *
 * Mirrors iOS's `SiteMetric`: each entry owns a stable [storageKey] (persisted across sessions,
 * decoupled from the enum name so refactors don't invalidate saved selections), a [labelRes] and a
 * [color]. [VIEWS] has no [color] and falls back to the theme primary for continuity with the
 * previous views-only chart.
 */
enum class StatsMetric(
    val storageKey: String,
    @StringRes val labelRes: Int,
    val color: Color?
) {
    VIEWS("views", R.string.stats_views, null),
    VISITORS("visitors", R.string.stats_visitors, StatsColors.MetricVisitors),
    LIKES("likes", R.string.stats_likes, StatsColors.MetricLikes),
    COMMENTS("comments", R.string.stats_comments, StatsColors.MetricComments),
    POSTS("posts", R.string.posts, StatsColors.MetricPosts);

    companion object {
        /** Used when the user has never picked a metric for the site. */
        val DEFAULT = VIEWS

        fun fromStorageKey(key: String?): StatsMetric? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * Chart data containing current and previous period series for the selected metric.
 */
data class ViewsStatsChartData(
    val currentPeriod: List<ChartDataPoint>,
    val previousPeriod: List<ChartDataPoint>
)

/**
 * A single data point for the chart.
 * @param label The formatted label for this time unit (e.g., "14:00", "Jan 15", "Jan")
 * @param value The value of the currently selected metric for this time unit
 */
data class ChartDataPoint(
    val label: String,
    val value: Long,
    val rawPeriod: String = ""
)

/**
 * A stat item for the bottom stats row. Also acts as the chart's metric selector, so it carries the
 * [metric] it represents, which drives its icon, label ([StatsMetric.labelRes]) and accent color.
 * @param metric The metric this item represents
 * @param value The stat value
 * @param change The change compared to previous period
 */
data class StatItem(
    val metric: StatsMetric,
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
