package org.wordpress.android.ui.newstats.util

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val FORMAT_MILLION = "%.1fM"
private const val FORMAT_THOUSAND = "%.1fK"
private const val MONTH_ABBREVIATION_LENGTH = 3

private const val DISPLAY_DATE_PATTERN = "MMM d, yyyy"
private val API_DATE_TIME_FORMAT = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

/**
 * Formats an API timestamp ("yyyy-MM-dd HH:mm:ss", site timezone) for display, e.g. "Aug 4, 2026".
 * Returns the input unchanged if it can't be parsed.
 */
fun formatStatsDateTime(dateTime: String): String = parseOrLog(dateTime) {
    LocalDateTime.parse(it, API_DATE_TIME_FORMAT)
        .format(displayDateFormat())
}

/**
 * Formats an API day ("yyyy-MM-dd") for display, e.g. "Aug 4, 2026". Returns the input unchanged
 * if it can't be parsed.
 */
fun formatStatsDate(date: String): String = parseOrLog(date) {
    LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
        .format(displayDateFormat())
}

private fun displayDateFormat() = DateTimeFormatter.ofPattern(
    DISPLAY_DATE_PATTERN,
    Locale.getDefault()
)

@Suppress("TooGenericExceptionCaught")
private inline fun parseOrLog(
    value: String,
    format: (String) -> String
): String = try {
    format(value)
} catch (e: Exception) {
    AppLog.w(
        AppLog.T.STATS,
        "Failed to parse stats date '$value': ${e.message}"
    )
    value
}

/**
 * Formats a stat value for display, grouped in full below [abbreviateFrom] and with a K/M suffix
 * at or above it. Examples at the default threshold: 500 -> "500", 1500 -> "1.5K",
 * 2500000 -> "2.5M"; at [TEN_THOUSAND]: 1986 -> "1,986".
 *
 * Cards are width-constrained and abbreviate early; detail screens pass [TEN_THOUSAND] so figures
 * stay exact for longer, which is the threshold the old stats screens use.
 */
fun formatStatValue(
    value: Long,
    abbreviateFrom: Int = THOUSAND
): String = when {
    value >= MILLION -> String.format(
        Locale.getDefault(),
        FORMAT_MILLION,
        value / MILLION.toDouble()
    )
    value >= abbreviateFrom.coerceAtLeast(THOUSAND) ->
        String.format(
            Locale.getDefault(),
            FORMAT_THOUSAND,
            value / THOUSAND.toDouble()
        )
    else -> NumberFormat
        .getIntegerInstance(Locale.getDefault())
        .format(value)
}

/** Threshold for detail screens, which favour exact figures over width. */
const val TEN_THOUSAND = 10_000

/**
 * Formats a fractional change as a percentage, e.g. -0.25 -> "-25%".
 */
fun formatChangePercentage(fraction: Double): String =
    NumberFormat.getPercentInstance(Locale.getDefault())
        .apply { maximumFractionDigits = 0 }
        .format(fraction)

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
