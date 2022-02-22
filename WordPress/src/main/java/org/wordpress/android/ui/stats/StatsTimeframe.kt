package org.wordpress.android.ui.stats

import org.wordpress.android.R
import org.wordpress.android.WordPress

/**
 * Timeframes for the stats pages.
 */
enum class StatsTimeframe(private val labelResId: Int) {
    INSIGHTS(R.string.stats_insights),
    DAY(R.string.stats_timeframe_days),
    WEEK(R.string.stats_timeframe_weeks),
    MONTH(R.string.stats_timeframe_months),
    YEAR(R.string.stats_timeframe_years);

    val label: String
        get() = WordPress.getContext().getString(labelResId)
}
