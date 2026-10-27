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
    DateFormat.getBestDateTimePattern(locale, skeleton).withPlainYearFields()

/** The CLDR pattern letters that stand for a year, including the week-based and extended forms. */
private const val YEAR_FIELD_LETTERS = "yYuUr"

/**
 * This pattern with every year field rewritten as [java.time]'s plain calendar year, keeping each
 * run's length so a two-digit year stays two digits.
 *
 * ICU and [java.time] read the other year letters differently. ICU answers Scottish Gaelic's
 * [StatsDateSkeleton.MONTH_YEAR] with "LLL Y", and "Y" is the *week-based* year in [java.time]: the
 * last days of December belong to the next ISO week-year, so a window ending 30 December 2024 would
 * be dated "Dùbh 2025" (CMM-2540). "U" (cyclic year name) and "r" (related year) are not
 * [java.time] fields at all and would throw on pattern creation.
 */
private fun String.withPlainYearFields(): String {
    val years = fieldRuns(this).filter { this[it.first] in YEAR_FIELD_LETTERS && this[it.first] != 'y' }
    if (years.isEmpty()) return this
    val chars = toCharArray()
    years.forEach { run -> run.forEach { chars[it] = 'y' } }
    return String(chars)
}

/**
 * The formatter for [yearSkeleton]'s fields *minus* its year, so the year-less end of a span is
 * written in exactly the same month form as the end that carries the year.
 *
 * Resolving a year-less skeleton separately does not guarantee that: ICU answers
 * [StatsDateSkeleton.MONTH] with the locale's *standalone* month form and
 * [StatsDateSkeleton.MONTH_YEAR] with its *format* one, and the two can disagree on width or even
 * on name-versus-number. Slovak gives "LLL" (jan) against "M/y" (10/2026), so a span built from
 * both read "jan - 10/2026" — a month name against a month number (CMM-2540). Czech mixed
 * abbreviated with full ("led - říjen"); nine shipped locales differed in all.
 *
 * Returns null when the year sits between two other fields, where dropping it would strand a
 * separator mid-pattern; the caller then falls back to resolving the year-less skeleton.
 */
fun yearlessFormatter(
    yearSkeleton: String,
    locale: Locale = Locale.getDefault()
): DateTimeFormatter? = yearlessPattern(statsDatePattern(yearSkeleton, locale))
    ?.let { DateTimeFormatter.ofPattern(it, locale) }

private fun yearlessPattern(pattern: String): String? {
    val fields = fieldRuns(pattern)
    if (fields.size < 2) return null
    val yearIndex = fields.indexOfFirst { pattern[it.first] in YEAR_FIELD_LETTERS }
    return when (yearIndex) {
        // Year last ("MMM y", "M/y", "MMM 'de' y"): keep up to the preceding field's end.
        fields.lastIndex -> pattern.substring(0, fields[yearIndex - 1].last + 1)
        // Year first ("y年M月", "y. 'g'. MMM"): keep from the following field's start.
        0 -> pattern.substring(fields[1].first)
        else -> null
    }
}

/**
 * The index ranges of [pattern]'s field runs, skipping quoted literals. Only ASCII letters count as
 * fields, so a locale that writes a literal in its own script ("年" in ja, "년" in ko) is not
 * mistaken for one.
 */
private fun fieldRuns(pattern: String): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var index = 0
    while (index < pattern.length) {
        val char = pattern[index]
        when {
            char == '\'' -> index = skipQuotedLiteral(pattern, index)
            char.isPatternField() -> {
                val start = index
                while (index < pattern.length && pattern[index] == char) index++
                runs += start until index
            }
            else -> index++
        }
    }
    return runs
}

/**
 * The index just past the literal opening at [openIndex], tolerating the "''" escape a pattern uses
 * for a literal apostrophe: both of its quotes are stepped over, so the second one cannot be read
 * as the one that closes the literal.
 */
private fun skipQuotedLiteral(pattern: String, openIndex: Int): Int {
    var index = openIndex + 1
    while (index < pattern.length) {
        if (pattern[index] == '\'') {
            index++
            if (index < pattern.length && pattern[index] == '\'') {
                index++
                continue
            }
            return index
        }
        index++
    }
    return index
}

private fun Char.isPatternField(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

/**
 * The formatter for the day alone, written as the locale's own [StatsDateSkeleton.DAY_MONTH] pattern
 * writes it: the day field run plus the ordinal dot that follows it, if the locale puts one there.
 *
 * Derived from that pattern rather than resolved from a bare "d" skeleton so the two ends of a
 * collapsed span agree. ICU puts the ordinal dot in the bare day skeleton for Czech, Slovak, Danish
 * and six more, but not for German, Bulgarian, Icelandic, Latvian or Serbian, whose day-month
 * pattern carries it all the same — so those five read "28-30. Juli" where German writes
 * "28.-30. Juli" (CMM-2540).
 *
 * Only the dot carries over. A word the locale writes between the day and the month belongs to the
 * month ("d 'de' MMM" in Galician, "d בMMM" in Hebrew), and so does a field separator ("d/MM" in
 * European Portuguese); keeping either would read "28 de-30 de xul." or "28/-30/07".
 */
fun dayOnlyFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        dayOnlyPattern(statsDatePattern(StatsDateSkeleton.DAY_MONTH, locale)),
        locale
    )

private fun dayOnlyPattern(pattern: String): String {
    val dayRun = fieldRuns(pattern).firstOrNull { pattern[it.first] == 'd' } ?: return "d"
    var end = dayRun.last + 1
    while (end < pattern.length && (pattern[end] == '.' || pattern[end].isWhitespace())) end++
    return pattern.substring(dayRun.first, end).trimEnd()
}

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
     * literal ("MMM d." in Hungarian, "MMM d일" in Korean) or a per-field suffix ("M月d日") — or the
     * pattern already writes [COLLAPSED_SEPARATOR] between its own fields. These spell the month
     * out on both ends ("8月28日 - 8月30日") rather than collapse.
     */
    ENCLOSED
}

/** The separator a collapsed span joins its two ends with ("28-30 Jul"). */
const val COLLAPSED_SEPARATOR = "-"

/**
 * Reads [DayFieldPosition] off the locale's own day-month pattern rather than off a list of
 * locales, so a locale nobody tested still collapses correctly or, failing that, falls back to the
 * always-readable [ENCLOSED][DayFieldPosition.ENCLOSED] form.
 */
fun dayFieldPosition(locale: Locale = Locale.getDefault()): DayFieldPosition {
    val pattern = statsDatePattern(StatsDateSkeleton.DAY_MONTH, locale)
    return when {
        // Uzbek writes "d-MMM", so a collapsed "28-30-iyl" would leave the range marker and the
        // pattern's own separator indistinguishable. Spell both ends out instead (CMM-2540).
        pattern.writesLiteral(COLLAPSED_SEPARATOR) -> DayFieldPosition.ENCLOSED
        pattern.firstOrNull() == 'd' -> DayFieldPosition.LEADING
        pattern.lastOrNull() == 'd' -> DayFieldPosition.TRAILING
        else -> DayFieldPosition.ENCLOSED
    }
}

/**
 * Whether this pattern renders any character of [separator] as a literal, counting the ones inside
 * quotes: those reach the output just the same, so they are equally ambiguous in a collapsed span.
 */
private fun String.writesLiteral(separator: String): Boolean {
    var index = 0
    var found = false
    while (index < length && !found) {
        val char = this[index]
        when {
            char == '\'' -> {
                val end = skipQuotedLiteral(this, index)
                found = substring(index, end).any { it in separator }
                index = end
            }
            char.isPatternField() -> index++
            else -> {
                found = char in separator
                index++
            }
        }
    }
    return found
}

/**
 * Whether [locale] opens a full date with the year, as Japanese ("2026年8月11日"), Chinese, Korean
 * and Hungarian do. Read off the locale's own full-date pattern rather than off a list of locales,
 * for the same reason as [dayFieldPosition].
 */
fun yearLeadsDate(locale: Locale = Locale.getDefault()): Boolean =
    statsDatePattern(StatsDateSkeleton.DAY_MONTH_YEAR, locale).firstOrNull() == 'y'
