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

private val API_DATE_TIME_FORMAT = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

/**
 * Formats an API timestamp ("yyyy-MM-dd HH:mm:ss", site timezone) for display, e.g. "Aug 4, 2026"
 * in en-US and "2026年8月4日" in Japanese. Returns the input unchanged if it can't be parsed.
 */
fun formatStatsDateTime(dateTime: String): String = parseOrLog(dateTime) {
    LocalDateTime.parse(it, API_DATE_TIME_FORMAT)
        .format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH_YEAR))
}

/**
 * Formats an API day ("yyyy-MM-dd") for display, e.g. "Aug 4, 2026". Returns the input unchanged
 * if it can't be parsed.
 */
fun formatStatsDate(date: String): String = parseOrLog(date) {
    LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
        .format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH_YEAR))
}

/**
 * Formats an API day ("yyyy-MM-dd") without its year, e.g. "Aug 4" in en-US and "4 Aug" in en-GB,
 * for a date whose year is already established by the dates around it. Returns the input unchanged
 * if it can't be parsed.
 */
fun formatStatsDayMonth(date: String): String = parseOrLog(date) {
    LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
        .format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH))
}

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
 * The figure in full, however large, e.g. 1200 -> "1,200" and 2500000 -> "2,500,000".
 *
 * For what a screen reader is given to read out: a description has no width to save, and the
 * figure it reads should be the one the sentence around it was built from -- a plural is chosen
 * on the real count, so speaking "1.2K" would announce a number the grammar does not agree with.
 */
fun formatStatValueExact(value: Long): String = NumberFormat
    .getIntegerInstance(Locale.getDefault())
    .format(value)

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
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.ThisWeek -> resourceProvider.getString(R.string.stats_period_this_week)
        is StatsPeriod.ThisMonth -> resourceProvider.getString(R.string.stats_period_this_month)
        is StatsPeriod.ThisYear -> resourceProvider.getString(R.string.stats_period_this_year)
        is StatsPeriod.Custom -> formatCustomDateRange(startDate, endDate)
    }
}

/** The separator between the two ends of a span that names its month (or year) on both ends. */
private const val RANGE_SEPARATOR = " - "

/**
 * Formats a Custom range's dates identically wherever a year-less range is shown (the Traffic
 * top-bar label and every detail card), so the two never diverge in day/month ordering or locale.
 *
 * A single-day range (the result of day-level paging) renders as one date; a range within one
 * calendar month names the month once where the locale's own date pattern allows it ("28-30 Jul"
 * in en-GB, "Jul 28-30" in en-US); and one that crosses a month boundary names the month on both
 * ends ("28 Jul - 3 Aug"), so a span like Jul 28 - Aug 3 is never mislabelled "28-3 Aug". See
 * [dayFieldPosition] for the locales that spell both ends out even inside one month.
 */
fun formatCustomDateRange(startDate: LocalDate, endDate: LocalDate): String {
    val dayMonth = statsDateFormatter(StatsDateSkeleton.DAY_MONTH)
    if (startDate == endDate) return startDate.format(dayMonth)

    val day = dayOnlyFormatter()
    val sameMonth = startDate.month == endDate.month && startDate.year == endDate.year
    return when (if (sameMonth) dayFieldPosition() else DayFieldPosition.ENCLOSED) {
        DayFieldPosition.LEADING ->
            "${startDate.format(day)}$COLLAPSED_SEPARATOR${endDate.format(dayMonth)}"
        DayFieldPosition.TRAILING ->
            "${startDate.format(dayMonth)}$COLLAPSED_SEPARATOR${endDate.format(day)}"
        DayFieldPosition.ENCLOSED ->
            "${startDate.format(dayMonth)}$RANGE_SEPARATOR${endDate.format(dayMonth)}"
    }
}

/**
 * The day span a chart legend or the top-bar label names, with its year: "Jul 28 - Jul 30, 2024"
 * in en-US, "28-30 Jul 2024" in en-GB, "2024年7月28日 - 7月30日" in Japanese. See
 * [rangeYearPlacement] for which end carries the year and [formatCustomDateRange] for the year-less
 * form the detail-card subtitles use.
 */
fun formatDayRangeWithYear(startDate: LocalDate, endDate: LocalDate): String = formatRangeWithYear(
    startDate = startDate,
    endDate = endDate,
    skeleton = StatsDateSkeleton.DAY_MONTH,
    yearSkeleton = StatsDateSkeleton.DAY_MONTH_YEAR,
    collapsible = startDate.year == endDate.year && startDate.month == endDate.month
)

/**
 * The month span a month-bucketed chart legend names, with its year: "Jan - Jun 2024",
 * "2024年1月 - 6月". A one-month span collapses to that month alone ("Jan 2024").
 */
fun formatMonthRangeWithYear(startDate: LocalDate, endDate: LocalDate): String = when {
    startDate.year == endDate.year && startDate.month == endDate.month ->
        startDate.format(statsDateFormatter(StatsDateSkeleton.MONTH_YEAR))
    else -> formatRangeWithYear(
        startDate = startDate,
        endDate = endDate,
        skeleton = StatsDateSkeleton.MONTH,
        yearSkeleton = StatsDateSkeleton.MONTH_YEAR,
        collapsible = false,
        matchYearlessEndToYearPattern = true
    )
}

/**
 * Renders a start–end span with the year on whichever end [rangeYearPlacement] puts it, the other
 * end rendered from the year-less [skeleton].
 *
 * When [collapsible] (the whole span sits in one month) the year-less end can shrink further to the
 * day alone — but only when it is the end the locale's day-month pattern lets go of, which is never
 * the end already carrying the year. So en-GB collapses "28-30 Jul 2024" while en-US, whose year
 * and day both sit at the end of a date, spells both ends out as "Jul 28 - Jul 30, 2024".
 *
 * Only a day span is ever [collapsible]: the day is the field that collapsing drops, and a month
 * span has none to drop.
 *
 * [matchYearlessEndToYearPattern] derives the year-less end from [yearSkeleton]'s own resolved
 * pattern (see [yearlessFormatter]) instead of resolving [skeleton] separately, so the two ends
 * cannot disagree on how the locale writes a month. Only the month span sets it: a *day* span's two
 * skeletons already agree everywhere, and deriving there would drop the ordinal dot Czech and
 * Slovak put on the day ("d. M." resolved alone, "d. M" derived from "d. M. y").
 */
private fun formatRangeWithYear(
    startDate: LocalDate,
    endDate: LocalDate,
    skeleton: String,
    yearSkeleton: String,
    collapsible: Boolean,
    matchYearlessEndToYearPattern: Boolean = false
): String {
    val withYear = statsDateFormatter(yearSkeleton)
    val placement = rangeYearPlacement(startDate.year, endDate.year)
    if (placement == RangeYearPlacement.BOTH) {
        return "${startDate.format(withYear)}$RANGE_SEPARATOR${endDate.format(withYear)}"
    }
    val yearOnStart = placement == RangeYearPlacement.LEADING
    val dayPosition = dayFieldPosition()
    val collapses = collapsible && when (dayPosition) {
        DayFieldPosition.LEADING -> !yearOnStart
        DayFieldPosition.TRAILING -> yearOnStart
        DayFieldPosition.ENCLOSED -> false
    }
    val plain = when {
        collapses -> dayOnlyFormatter()
        matchYearlessEndToYearPattern -> yearlessFormatter(yearSkeleton)
            ?: statsDateFormatter(skeleton)
        else -> statsDateFormatter(skeleton)
    }
    val separator = if (collapses) COLLAPSED_SEPARATOR else RANGE_SEPARATOR
    return if (yearOnStart) {
        "${startDate.format(withYear)}$separator${endDate.format(plain)}"
    } else {
        "${startDate.format(plain)}$separator${endDate.format(withYear)}"
    }
}

/**
 * Where the year should be rendered on a start–end date range so the span reads unambiguously and
 * in the order the reader's locale writes a date: [BOTH] endpoints when they fall in different
 * calendar years, and otherwise the single endpoint the locale's own full-date pattern leads with —
 * [LEADING] where the year comes first ("2024年7月28日 - 7月30日"), [TRAILING] everywhere else, with
 * the leading endpoint inheriting it ("28 Jul - 3 Aug 2024"). This is the single source of truth for
 * that rule, shared by the Traffic top-bar label ([formatCustomDateRangeTwoLine]) and the Views-card
 * legend, so the two can't drift.
 */
enum class RangeYearPlacement { LEADING, TRAILING, BOTH }

fun rangeYearPlacement(startYear: Int, endYear: Int): RangeYearPlacement = when {
    startYear != endYear -> RangeYearPlacement.BOTH
    yearLeadsDate() -> RangeYearPlacement.LEADING
    else -> RangeYearPlacement.TRAILING
}

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
 * The day-month line follows the locale's own ordering, exactly as [formatCustomDateRange] does;
 * only a span the locale lets collapse onto one month stays a [RangeLine.Single].
 */
data class DateRangeLabel(val primary: RangeLine, val secondary: RangeLine?)

fun formatCustomDateRangeTwoLine(startDate: LocalDate, endDate: LocalDate, currentYear: Int): DateRangeLabel {
    val dayMonth = statsDateFormatter(StatsDateSkeleton.DAY_MONTH)
    val sameMonth = startDate.month == endDate.month && startDate.year == endDate.year
    val primary = when {
        startDate == endDate -> RangeLine.Single(startDate.format(dayMonth))
        sameMonth && dayFieldPosition() != DayFieldPosition.ENCLOSED ->
            RangeLine.Single(formatCustomDateRange(startDate, endDate))
        else -> RangeLine.Split(startDate.format(dayMonth), endDate.format(dayMonth))
    }
    val secondary = when (rangeYearPlacement(startDate.year, endDate.year)) {
        RangeYearPlacement.BOTH -> RangeLine.Split("${startDate.year}", "${endDate.year}")
        // Same year: suppress it entirely within the current year, otherwise show it once. The year
        // is on its own line here, so it does not matter which end the locale would attach it to.
        RangeYearPlacement.LEADING, RangeYearPlacement.TRAILING ->
            if (startDate.year == currentYear) null else RangeLine.Single("${startDate.year}")
    }
    return DateRangeLabel(primary, secondary)
}

/**
 * The resolved date window a *preset* period covers, rendered as the Traffic selector's second
 * line so the label always says which dates are on screen — the preset name alone ("Last 7 days")
 * never changes, which is what made a stale, still-selected period look like stats that had stopped
 * updating (CMM-2540).
 *
 * [monthGranularity] periods (Last 12 Months, This Year) name their endpoints by month, matching
 * the buckets their chart is drawn in; the rest name whole days.
 */
fun formatPresetRange(startDate: LocalDate, endDate: LocalDate, monthGranularity: Boolean): String = when {
    startDate == endDate -> startDate.format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH_YEAR))
    monthGranularity -> formatMonthRangeWithYear(startDate, endDate)
    else -> formatDayRangeWithYear(startDate, endDate)
}

/**
 * The full range spoken as a single string for accessibility, with the year merged onto each
 * endpoint so a screen reader reads it as one date range instead of the four-to-six separate text
 * nodes the two-line layout renders. [rangeTemplate] is a "from – to" template (two `%s` args);
 * labels that aren't a range (a single day, a within-month span) are returned as-is.
 *
 * [yearLeads] is [yearLeadsDate] for the reader's locale and decides which endpoint a year attaches
 * to. The two-line layout can ignore that, since the year has a line of its own there and so never
 * meets the dates in one string; merging the lines for a screen reader brings the order back.
 * Japanese opens a date with its year, so a past-year span reads "2024 6月14日 to 6月20日" rather than
 * "6月14日 to 6月20日 2024". Taken as a parameter rather than read here so this stays a pure function,
 * testable without an ICU lookup.
 */
fun DateRangeLabel.toContentDescription(rangeTemplate: String, yearLeads: Boolean): String {
    val p = primary
    val s = secondary
    return when {
        // A preset: its name over the dates it resolved to, which is not a year line.
        p is RangeLine.Single && s is RangeLine.Single -> "${p.text} ${s.text}"
        p is RangeLine.Split && s == null -> rangeTemplate.format(p.start, p.end)
        p is RangeLine.Split && s is RangeLine.Single -> if (yearLeads) {
            rangeTemplate.format(p.start.withYear(s.text, yearLeads), p.end)
        } else {
            rangeTemplate.format(p.start, p.end.withYear(s.text, yearLeads))
        }
        p is RangeLine.Split && s is RangeLine.Split -> rangeTemplate.format(
            p.start.withYear(s.start, yearLeads),
            p.end.withYear(s.end, yearLeads)
        )
        p is RangeLine.Single -> p.text
        else -> rangeTemplate.format((p as RangeLine.Split).start, p.end)
    }
}

/** This endpoint with [year] on the side the locale writes it: ahead of it where the year leads. */
private fun String.withYear(year: String, yearLeads: Boolean): String =
    if (yearLeads) "$year $this" else "$this $year"
