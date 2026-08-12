package org.wordpress.android.ui.newstats.util

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val FORMAT_MILLION = "%.1fM"
private const val FORMAT_THOUSAND = "%.1fK"

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

private const val FORMAT_DECIMAL = "%.1f"

fun formatStatValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.getDefault(), FORMAT_DECIMAL, value)
    }
}

/**
 * Formats an email stat value for display, showing "-" for zero values.
 */
fun formatEmailStat(value: Long): String {
    return if (value == 0L) "-" else formatStatValue(value)
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d", Locale.getDefault())
private val DAY_MONTH_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/**
 * Converts a StatsPeriod to a human-readable date range string.
 *
 * A Custom range within one calendar month renders as "28-30 Jul"; one that crosses a month
 * boundary renders the month on both ends ("28 Jul - 3 Aug") so a span like Jul 28 - Aug 3 is no
 * longer mislabelled "28-3 Aug".
 */
fun StatsPeriod.toDateRangeString(resourceProvider: ResourceProvider): String {
    return when (this) {
        is StatsPeriod.Today -> resourceProvider.getString(R.string.stats_period_today)
        is StatsPeriod.Last7Days -> resourceProvider.getString(R.string.stats_period_last_7_days)
        is StatsPeriod.Last30Days -> resourceProvider.getString(R.string.stats_period_last_30_days)
        is StatsPeriod.Last6Months -> resourceProvider.getString(R.string.stats_period_last_6_months)
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.Custom -> if (startDate.month == endDate.month && startDate.year == endDate.year) {
            "${startDate.format(DAY_FORMAT)}-${endDate.format(DAY_MONTH_FORMAT)}"
        } else {
            "${startDate.format(DAY_MONTH_FORMAT)} - ${endDate.format(DAY_MONTH_FORMAT)}"
        }
    }
}
