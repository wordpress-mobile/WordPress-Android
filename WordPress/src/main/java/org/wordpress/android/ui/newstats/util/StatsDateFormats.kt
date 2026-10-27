package org.wordpress.android.ui.newstats.util

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The CLDR skeletons every New Stats date is rendered from.
 *
 * A skeleton names the *fields* a date should show, not the order they are written in;
 * [statsDateFormatter] resolves it into the pattern the reader's locale actually uses for those
 * fields. So the same [DAY_MONTH] skeleton reads "Aug 11" in en-US, "11 Aug" in en-GB and "8月11日"
 * in Japanese, instead of the one hardcoded day-month order every stats screen used to share
 * (CMM-2540).
 *
 * Years are deliberately *not* in here: a bare four-digit year is unambiguous everywhere, and the
 * localized form ("2026年", "2026.") only costs width in the cramped app bar and chart axis where
 * stats show one, so those stay plain digits.
 */
object StatsDateSkeleton {
    /** Day of month alone, for the collapsed end of a span inside one month ("28-30 Jul"). */
    const val DAY = "d"

    /** Abbreviated month alone, for month-bucket chart labels. */
    const val MONTH = "MMM"

    /** Day and abbreviated month, the default for a date shown without its year. */
    const val DAY_MONTH = "MMMd"

    /** Day and full month, for headline dates that have room for it. */
    const val DAY_FULL_MONTH = "MMMMd"

    /** Day, abbreviated month and year — a date that has to stand on its own. */
    const val DAY_MONTH_YEAR = "yMMMd"

    /** Abbreviated month and year, for a month-granularity span's endpoints. */
    const val MONTH_YEAR = "yMMM"

    /** Weekday, day and abbreviated month, for the Today card's subtitle. */
    const val WEEKDAY_DAY_MONTH = "EEEEMMMd"
}

/**
 * The formatter that writes [skeleton]'s fields the way [locale] writes them.
 *
 * Formatters are built per call rather than cached in a field so they always honour the current
 * [Locale.getDefault]; caching one captures the locale at class-load and trips Lint's
 * ConstantLocale check.
 */
fun statsDateFormatter(
    skeleton: String,
    locale: Locale = Locale.getDefault()
): DateTimeFormatter = DateTimeFormatter.ofPattern(statsDatePattern(skeleton, locale), locale)

private fun statsDatePattern(skeleton: String, locale: Locale): String =
    DateFormat.getBestDateTimePattern(locale, skeleton)

/**
 * Where the day of month sits in a locale's [StatsDateSkeleton.DAY_MONTH] pattern, which decides
 * how a span inside a single month can name that month only once.
 */
enum class DayFieldPosition {
    /** The pattern opens with the day ("d MMM"), so the span collapses as "28-30 Jul". */
    LEADING,

    /** The pattern closes with the day ("MMM d"), so the span collapses as "Jul 28-30". */
    TRAILING,

    /**
     * The day is wrapped by something the collapsed form would drop or strand — a trailing
     * literal ("MMM d." in Hungarian, "MMM d일" in Korean) or a per-field suffix ("M月d日"). These
     * spell the month out on both ends ("8月28日 - 8月30日") rather than collapse.
     */
    ENCLOSED
}

/**
 * Reads [DayFieldPosition] off the locale's own day-month pattern rather than off a list of
 * locales, so a locale nobody tested still collapses correctly or, failing that, falls back to the
 * always-readable [ENCLOSED][DayFieldPosition.ENCLOSED] form.
 */
fun dayFieldPosition(locale: Locale = Locale.getDefault()): DayFieldPosition {
    val pattern = statsDatePattern(StatsDateSkeleton.DAY_MONTH, locale)
    return when {
        pattern.firstOrNull() == 'd' -> DayFieldPosition.LEADING
        pattern.lastOrNull() == 'd' -> DayFieldPosition.TRAILING
        else -> DayFieldPosition.ENCLOSED
    }
}

/**
 * Whether [locale] opens a full date with the year, as Japanese ("2026年8月11日"), Chinese, Korean
 * and Hungarian do. Read off the locale's own full-date pattern rather than off a list of locales,
 * for the same reason as [dayFieldPosition].
 */
fun yearLeadsDate(locale: Locale = Locale.getDefault()): Boolean =
    statsDatePattern(StatsDateSkeleton.DAY_MONTH_YEAR, locale).firstOrNull() == 'y'
