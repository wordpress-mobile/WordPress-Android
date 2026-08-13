package org.wordpress.android.ui.newstats.util

import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
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

/**
 * Converts a StatsPeriod to a human-readable date range string. Custom ranges are formatted by the
 * shared [formatCustomDateRange] (the compact, single-line, year-less form used on the detail-card
 * subtitles). The Traffic top-bar selector instead uses the richer two-line [formatCustomDateRangeTwoLine]
 * so it can show the year, so the two intentionally differ.
 */
fun StatsPeriod.toDateRangeString(resourceProvider: ResourceProvider): String {
    return when (this) {
        is StatsPeriod.Today -> resourceProvider.getString(R.string.stats_period_today)
        is StatsPeriod.Last7Days -> resourceProvider.getString(R.string.stats_period_last_7_days)
        is StatsPeriod.Last30Days -> resourceProvider.getString(R.string.stats_period_last_30_days)
        is StatsPeriod.Last6Months -> resourceProvider.getString(R.string.stats_period_last_6_months)
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.Custom -> formatCustomDateRange(startDate, endDate)
    }
}

/**
 * Formats a Custom range's dates identically wherever a range is shown (the Traffic top-bar label
 * and every detail card), so the two never diverge in day/month ordering or locale.
 *
 * A single-day range (the result of day-level paging) renders as one date ("11 Aug"); a range
 * within one calendar month renders as "28-30 Jul"; one that crosses a month boundary renders the
 * month on both ends ("28 Jul - 3 Aug") so a span like Jul 28 - Aug 3 is no longer mislabelled
 * "28-3 Aug".
 *
 * The formatters are built per call (rather than as static fields) so they always honour the
 * current [Locale.getDefault]; caching them in a top-level `val` captures the locale at class-load
 * and trips Android Lint's ConstantLocale check.
 */
fun formatCustomDateRange(startDate: LocalDate, endDate: LocalDate): String {
    val dayFormat = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    return when {
        startDate == endDate -> startDate.format(dayMonthFormat)
        startDate.month == endDate.month && startDate.year == endDate.year ->
            "${startDate.format(dayFormat)}-${endDate.format(dayMonthFormat)}"
        else -> "${startDate.format(dayMonthFormat)} - ${endDate.format(dayMonthFormat)}"
    }
}

/**
 * Where the year should be rendered on a start–end date range so the span reads unambiguously:
 * [BOTH] endpoints when they fall in different calendar years, the [TRAILING] endpoint alone when
 * they share a year (the leading endpoint inherits it, e.g. "28 Jul - 3 Aug 2024"). This is the
 * single source of truth for that rule, shared by the Traffic top-bar label
 * ([formatCustomDateRangeTwoLine]) and the Views-card legend, so the two can't drift.
 */
enum class RangeYearPlacement { TRAILING, BOTH }

fun rangeYearPlacement(startYear: Int, endYear: Int): RangeYearPlacement =
    if (startYear == endYear) RangeYearPlacement.TRAILING else RangeYearPlacement.BOTH

/**
 * One rendered line of a two-line date-range label. [Split] carries the two endpoints separately so
 * the UI can align both lines on the "-" separator; [Single] is a standalone line (a single day, a
 * within-a-month day span, or a single year) with nothing to align, so it is centered.
 */
sealed interface RangeLine {
    data class Single(val text: String) : RangeLine
    data class Split(val start: String, val end: String) : RangeLine
}

/**
 * The Traffic top-bar selector's label for a Custom range, split across two lines so the full range
 * (including the year) fits the cramped app-bar width: [primary] is the day-month range, [secondary]
 * the year(s). Kept structured (rather than pre-joined) so the selector can align both lines on the
 * "-" — see [RangeLine].
 *
 * [secondary] is null — so the label collapses to a single line — when the whole range falls within
 * [currentYear], since the year adds nothing there. A range in a different (past) year shows that
 * single year; a range that crosses years always shows both, even when one of them is the current
 * year, because both are needed to read the span. [secondary] is likewise null for preset periods,
 * which render as a single line.
 *
 * Formatters are built per call to honour the current [Locale.getDefault], matching
 * [formatCustomDateRange]; caching them trips Android Lint's ConstantLocale check.
 */
data class DateRangeLabel(val primary: RangeLine, val secondary: RangeLine?)

fun formatCustomDateRangeTwoLine(startDate: LocalDate, endDate: LocalDate, currentYear: Int): DateRangeLabel {
    val dayFormat = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val sameYear = startDate.year == endDate.year
    val primary = when {
        startDate == endDate -> RangeLine.Single(startDate.format(dayMonthFormat))
        startDate.month == endDate.month && sameYear ->
            RangeLine.Single("${startDate.format(dayFormat)}-${endDate.format(dayMonthFormat)}")
        else -> RangeLine.Split(startDate.format(dayMonthFormat), endDate.format(dayMonthFormat))
    }
    val secondary = when (rangeYearPlacement(startDate.year, endDate.year)) {
        RangeYearPlacement.BOTH -> RangeLine.Split("${startDate.year}", "${endDate.year}")
        // Same year: suppress it entirely within the current year, otherwise show it once.
        RangeYearPlacement.TRAILING ->
            if (startDate.year == currentYear) null else RangeLine.Single("${startDate.year}")
    }
    return DateRangeLabel(primary, secondary)
}

/**
 * The full range spoken as a single string for accessibility, with the year merged onto each
 * endpoint so a screen reader reads it as one date range instead of the four-to-six separate text
 * nodes the two-line layout renders. [rangeTemplate] is a "from – to" template (two `%s` args);
 * labels that aren't a range (a single day, a within-month span) are returned as-is.
 */
fun DateRangeLabel.toContentDescription(rangeTemplate: String): String {
    val p = primary
    val s = secondary
    return when {
        p is RangeLine.Single && s is RangeLine.Single -> "${p.text} ${s.text}"
        p is RangeLine.Split && s == null -> rangeTemplate.format(p.start, p.end)
        p is RangeLine.Split && s is RangeLine.Single -> rangeTemplate.format(p.start, "${p.end} ${s.text}")
        p is RangeLine.Split && s is RangeLine.Split ->
            rangeTemplate.format("${p.start} ${s.start}", "${p.end} ${s.end}")
        p is RangeLine.Single -> p.text
        else -> rangeTemplate.format((p as RangeLine.Split).start, p.end)
    }
}
