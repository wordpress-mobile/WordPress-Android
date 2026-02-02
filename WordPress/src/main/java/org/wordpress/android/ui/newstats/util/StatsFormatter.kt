package org.wordpress.android.ui.newstats.util

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

private const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val FORMAT_MILLION = "%.1fM"
private const val FORMAT_THOUSAND = "%.1fK"
private const val MONTH_ABBREVIATION_LENGTH = 3

/**
 * Formats a stat value for display, using K/M suffixes for large numbers.
 * Examples: 1500 -> "1.5K", 2500000 -> "2.5M", 500 -> "500"
 */
fun formatStatValue(value: Long): String {
    return when {
        value >= MILLION -> String.format(Locale.getDefault(), FORMAT_MILLION, value / MILLION.toDouble())
        value >= THOUSAND -> String.format(Locale.getDefault(), FORMAT_THOUSAND, value / THOUSAND.toDouble())
        else -> value.toString()
    }
}

/**
 * Converts a StatsPeriod to a human-readable date range string.
 */
fun StatsPeriod.toDateRangeString(resourceProvider: ResourceProvider): String {
    return when (this) {
        is StatsPeriod.Today -> resourceProvider.getString(R.string.stats_period_today)
        is StatsPeriod.Last7Days -> resourceProvider.getString(R.string.stats_period_last_7_days)
        is StatsPeriod.Last30Days -> resourceProvider.getString(R.string.stats_period_last_30_days)
        is StatsPeriod.Last6Months -> resourceProvider.getString(R.string.stats_period_last_6_months)
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.Custom -> "${startDate.dayOfMonth}-${endDate.dayOfMonth} ${
            endDate.month.name.take(MONTH_ABBREVIATION_LENGTH).lowercase().replaceFirstChar { it.uppercase() }
        }"
    }
}
