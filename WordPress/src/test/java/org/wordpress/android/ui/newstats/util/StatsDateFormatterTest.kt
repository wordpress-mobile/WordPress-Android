package org.wordpress.android.ui.newstats.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.util.Locale

/**
 * Covers the three shapes a locale can give a date, each exercised through a real locale rather
 * than a fixture, because the whole point of [statsDateFormatter] is that the field order comes
 * from ICU and not from us:
 *
 * - en-GB writes the day first ("11 Aug"), so a span inside one month names the month once at the
 *   end ("28-30 Jul") and the year likewise lands on the end ("14-20 Jan 2024").
 * - en-US writes the month first ("Aug 11"), so that same span names the month once at the *start*
 *   ("Jul 28-30") — and when a year is in play, which also sits at the end of a US date, both ends
 *   are spelled out rather than stranding it.
 * - ja writes year-month-day with a suffix on every field ("2026年8月11日"), so nothing collapses
 *   and the year leads the span.
 *
 * Robolectric because that ICU lookup is a framework call — see [StatsDateUnitTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class StatsDateFormatterTest {
    private val resourceProvider = mock<ResourceProvider>()
    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun tearDown() {
        // setLocale mutates global state; restore it so it can't leak into the rest of the runner.
        Locale.setDefault(originalLocale)
    }

    private fun setLocale(locale: Locale) = Locale.setDefault(locale)

    private fun rangeOf(start: LocalDate, end: LocalDate) =
        StatsPeriod.Custom(start, end).toDateRangeString(resourceProvider)

    @Test
    fun `a single day is written in the locale's own field order`() {
        val day = LocalDate.of(2026, 8, 11)

        setLocale(Locale.UK)
        assertThat(rangeOf(day, day)).isEqualTo("11 Aug")

        setLocale(Locale.US)
        assertThat(rangeOf(day, day)).isEqualTo("Aug 11")

        setLocale(Locale.JAPAN)
        assertThat(rangeOf(day, day)).isEqualTo("8月11日")
    }

    @Test
    fun `a span inside one month names the month on the end the locale frees up`() {
        val start = LocalDate.of(2026, 7, 28)
        val end = LocalDate.of(2026, 7, 30)

        setLocale(Locale.UK)
        assertThat(rangeOf(start, end)).isEqualTo("28-30 Jul")

        setLocale(Locale.US)
        assertThat(rangeOf(start, end)).isEqualTo("Jul 28-30")

        // Japanese suffixes both fields, so a bare "30" would strand its 日.
        setLocale(Locale.JAPAN)
        assertThat(rangeOf(start, end)).isEqualTo("7月28日 - 7月30日")
    }

    @Test
    fun `a collapsed span keeps the ordinal dot the locale writes on its day`() {
        val start = LocalDate.of(2024, 7, 28)
        val end = LocalDate.of(2024, 7, 30)

        // German's day-month pattern is "d. MMM", but ICU's bare day skeleton is a plain "d", so
        // taking the collapsed end from that skeleton dropped the dot: "28-30. Juli" (CMM-2540).
        setLocale(Locale.GERMANY)
        assertThat(rangeOf(start, end)).isEqualTo("28.-30. Juli")
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("28.-30. Juli 2024")

        // Bulgarian writes the month as a number with the same dot ("d.MM").
        setLocale(Locale.forLanguageTag("bg"))
        assertThat(rangeOf(start, end)).isEqualTo("28.-30.07")
    }

    @Test
    fun `a collapsed span keeps a word the locale writes before the month on the month`() {
        val start = LocalDate.of(2024, 7, 28)
        val end = LocalDate.of(2024, 7, 30)

        // Only the ordinal dot belongs to the day. Galician's "d 'de' MMM" and Hebrew's "d בMMM"
        // put a word there that belongs to the month, so the collapsed end must not carry it.
        setLocale(Locale.forLanguageTag("gl"))
        assertThat(rangeOf(start, end)).isEqualTo("28-30 de xul.")

        setLocale(Locale.forLanguageTag("pt-BR"))
        assertThat(rangeOf(start, end)).isEqualTo("28-30 de jul.")
    }

    @Test
    fun `a span does not collapse when the locale's own pattern uses the range separator`() {
        // Uzbek writes "d-MMM", so collapsing to "28-30-iyl" would make the range marker
        // indistinguishable from the pattern's own separator (CMM-2540).
        setLocale(Locale.forLanguageTag("uz"))

        assertThat(rangeOf(LocalDate.of(2024, 7, 28), LocalDate.of(2024, 7, 30)))
            .isEqualTo("28-iyl - 30-iyl")
    }

    @Test
    fun `a span across a month boundary names the month on both ends`() {
        val start = LocalDate.of(2026, 7, 28)
        val end = LocalDate.of(2026, 8, 3)

        setLocale(Locale.UK)
        assertThat(rangeOf(start, end)).isEqualTo("28 Jul - 3 Aug")

        setLocale(Locale.US)
        assertThat(rangeOf(start, end)).isEqualTo("Jul 28 - Aug 3")

        setLocale(Locale.JAPAN)
        assertThat(rangeOf(start, end)).isEqualTo("7月28日 - 8月3日")
    }

    @Test
    fun `a span between the same month of two different years names the month on both ends`() {
        // The collapse guard checks the year as well as the month: without that, a span a year
        // apart would read "28-30 Jul" and hide the fact that it spans twelve months.
        val start = LocalDate.of(2024, 7, 28)
        val end = LocalDate.of(2025, 7, 30)

        setLocale(Locale.UK)
        assertThat(rangeOf(start, end)).isEqualTo("28 Jul - 30 Jul")

        setLocale(Locale.US)
        assertThat(rangeOf(start, end)).isEqualTo("Jul 28 - Jul 30")
    }

    @Test
    fun `a span across a year boundary still omits the year in the compact form`() {
        val start = LocalDate.of(2025, 12, 30)
        val end = LocalDate.of(2026, 1, 2)

        setLocale(Locale.UK)
        assertThat(rangeOf(start, end)).isEqualTo("30 Dec - 2 Jan")

        setLocale(Locale.JAPAN)
        assertThat(rangeOf(start, end)).isEqualTo("12月30日 - 1月2日")
    }

    @Test
    fun `a day span with its year puts the year where the locale writes it`() {
        val start = LocalDate.of(2024, 1, 14)
        val end = LocalDate.of(2024, 1, 20)

        setLocale(Locale.UK)
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("14-20 Jan 2024")

        // The US year and day both sit at the end of a date, so the span spells both ends out.
        setLocale(Locale.US)
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("Jan 14 - Jan 20, 2024")

        setLocale(Locale.JAPAN)
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("2024年1月14日 - 1月20日")
    }

    @Test
    fun `a day span across years carries the year on both ends`() {
        val start = LocalDate.of(2024, 12, 29)
        val end = LocalDate.of(2025, 1, 4)

        setLocale(Locale.UK)
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("29 Dec 2024 - 4 Jan 2025")

        setLocale(Locale.JAPAN)
        assertThat(formatDayRangeWithYear(start, end)).isEqualTo("2024年12月29日 - 2025年1月4日")
    }

    @Test
    fun `a month span names its year once`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 6, 1)

        setLocale(Locale.US)
        assertThat(formatMonthRangeWithYear(start, end)).isEqualTo("Jan - Jun 2024")

        setLocale(Locale.JAPAN)
        assertThat(formatMonthRangeWithYear(start, end)).isEqualTo("2024年1月 - 6月")
    }

    @Test
    fun `a month span writes both ends in the same month form`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 6, 1)

        // Slovak resolves a bare month to a name ("jan") but a month-with-year to a number
        // ("6/2024"), so taking each end from its own skeleton read "jan - 6/2024". Both ends now
        // come from the year-bearing pattern (CMM-2540).
        setLocale(Locale.forLanguageTag("sk"))
        assertThat(formatMonthRangeWithYear(start, end)).isEqualTo("1 - 6/2024")

        // Czech disagreed on width instead: abbreviated alone, full with the year.
        setLocale(Locale.forLanguageTag("cs"))
        assertThat(formatMonthRangeWithYear(start, end)).isEqualTo("leden - červen 2024")

        // German differed only in the abbreviation dot, which the derived end now keeps.
        setLocale(Locale.GERMANY)
        assertThat(formatMonthRangeWithYear(start, end)).isEqualTo("Jan. - Juni 2024")
    }

    @Test
    fun `a cross-year month span carries the year on both ends, so neither is derived`() {
        setLocale(Locale.forLanguageTag("sk"))

        assertThat(
            formatMonthRangeWithYear(LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 1))
        ).isEqualTo("12/2024 - 1/2025")
    }

    @Test
    fun `a day span keeps the day form the locale resolves on its own`() {
        // The day skeletons already agree, and deriving would drop the ordinal dot cs/sk put on
        // the day ("d. M." alone vs "d. M" derived from "d. M. y"), so the day path is left alone.
        setLocale(Locale.forLanguageTag("cs"))

        assertThat(
            formatDayRangeWithYear(LocalDate.of(2024, 7, 28), LocalDate.of(2024, 8, 3))
        ).isEqualTo("28. 7. - 3. 8. 2024")
    }

    @Test
    fun `a year-end month keeps its calendar year where ICU asks for the week-based one`() {
        // Scottish Gaelic resolves a month-with-year to "LLL Y", and "Y" is the week-based year in
        // java.time: December 2024 falls in ISO week-year 2025, so the label read a year ahead
        // until the pattern's year fields were normalized (CMM-2540).
        setLocale(Locale.forLanguageTag("gd"))
        val december = LocalDate.of(2024, 12, 30)

        assertThat(formatMonthRangeWithYear(december, december)).endsWith("2024")
    }

    @Test
    fun `a month span of one month collapses to that month`() {
        val month = LocalDate.of(2024, 1, 1)

        setLocale(Locale.US)
        assertThat(formatMonthRangeWithYear(month, month)).isEqualTo("Jan 2024")

        setLocale(Locale.JAPAN)
        assertThat(formatMonthRangeWithYear(month, month)).isEqualTo("2024年1月")
    }

    @Test
    fun `a preset's resolved window is named by day, or by month when it is bucketed that way`() {
        setLocale(Locale.UK)

        assertThat(
            formatPresetRange(
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 27),
                monthGranularity = false
            )
        ).isEqualTo("21-27 Oct 2026")

        assertThat(
            formatPresetRange(
                LocalDate.of(2025, 11, 1),
                LocalDate.of(2026, 10, 27),
                monthGranularity = true
            )
        ).isEqualTo("Nov 2025 - Oct 2026")
    }

    @Test
    fun `a preset covering a single day is named as that one date, with its year`() {
        val today = LocalDate.of(2026, 10, 27)

        setLocale(Locale.UK)
        assertThat(formatPresetRange(today, today, monthGranularity = false))
            .isEqualTo("27 Oct 2026")

        setLocale(Locale.JAPAN)
        assertThat(formatPresetRange(today, today, monthGranularity = false))
            .isEqualTo("2026年10月27日")
    }

    @Test
    fun `an API date is rendered in full`() {
        setLocale(Locale.US)
        assertThat(formatStatsDate("2026-08-04")).isEqualTo("Aug 4, 2026")
        assertThat(formatStatsDateTime("2026-08-04 13:45:00")).isEqualTo("Aug 4, 2026")

        setLocale(Locale.JAPAN)
        assertThat(formatStatsDate("2026-08-04")).isEqualTo("2026年8月4日")
    }

    @Test
    fun `an unparseable API date is returned unchanged`() {
        setLocale(Locale.US)
        assertThat(formatStatsDate("not-a-date")).isEqualTo("not-a-date")
        assertThat(formatStatsDateTime("not-a-timestamp")).isEqualTo("not-a-timestamp")
    }

    @Test
    fun `the two-line label keeps the locale's ordering and only collapses where it can`() {
        val start = LocalDate.of(2025, 6, 14)
        val end = LocalDate.of(2025, 6, 20)

        setLocale(Locale.UK)
        assertThat(formatCustomDateRangeTwoLine(start, end, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Single("14-20 Jun"))

        setLocale(Locale.US)
        assertThat(formatCustomDateRangeTwoLine(start, end, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Single("Jun 14-20"))

        // Nothing to collapse, so both ends land on the line as a Split the selector can align.
        setLocale(Locale.JAPAN)
        assertThat(formatCustomDateRangeTwoLine(start, end, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Split("6月14日", "6月20日"))
    }

    @Test
    fun `the two-line label renders a single day as one line in the locale's own order`() {
        val day = LocalDate.of(2025, 6, 14)

        setLocale(Locale.UK)
        assertThat(formatCustomDateRangeTwoLine(day, day, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Single("14 Jun"))

        setLocale(Locale.US)
        assertThat(formatCustomDateRangeTwoLine(day, day, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Single("Jun 14"))

        // Nothing collapses in Japanese, but a single day has no second endpoint to split out.
        setLocale(Locale.JAPAN)
        assertThat(formatCustomDateRangeTwoLine(day, day, currentYear = 2025).primary)
            .isEqualTo(RangeLine.Single("6月14日"))
    }

    @Test
    fun `the two-line label shows the year only outside the current one`() {
        setLocale(Locale.UK)
        val day = LocalDate.of(2024, 6, 14)

        assertThat(formatCustomDateRangeTwoLine(day, day, currentYear = 2025).secondary)
            .isEqualTo(RangeLine.Single("2024"))
        assertThat(formatCustomDateRangeTwoLine(day, day, currentYear = 2024).secondary).isNull()
    }

    @Test
    fun `the two-line label splits the year line across a year boundary`() {
        setLocale(Locale.UK)
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 12, 30),
            LocalDate.of(2026, 1, 2),
            currentYear = 2026
        )

        assertThat(label.primary).isEqualTo(RangeLine.Split("30 Dec", "2 Jan"))
        assertThat(label.secondary).isEqualTo(RangeLine.Split("2025", "2026"))
    }
}
