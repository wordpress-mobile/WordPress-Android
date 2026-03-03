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

data class GraphDataPoint(
    val label: String,
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
