package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import androidx.annotation.StringRes
import org.wordpress.android.R

sealed class SubscribersGraphUiState {
    data object Loading : SubscribersGraphUiState()
    data class Loaded(
        val dataPoints: List<GraphDataPoint>
    ) : SubscribersGraphUiState()
    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : SubscribersGraphUiState()
}

/**
 * One point on the subscribers chart. [label] is the compact axis form ("27 Jul"); [markerLabel]
 * is what the press-and-hold marker shows, which for a weekly point names the whole span
 * ("27 Jul - 2 Aug") so it cannot be mistaken for a single day.
 */
data class GraphDataPoint(
    val label: String,
    val markerLabel: String,
    val count: Long
)

@Suppress("MagicNumber")
enum class SubscribersGraphTab(
    val unit: String,
    val quantity: Int,
    @StringRes val labelResId: Int
) {
    DAYS("day", 30, R.string.stats_subscribers_graph_days),
    WEEKS("week", 12, R.string.stats_subscribers_graph_weeks),
    MONTHS(
        "month", 6, R.string.stats_subscribers_graph_months
    ),
    YEARS("year", 3, R.string.stats_subscribers_graph_years)
}
