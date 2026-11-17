package org.wordpress.android.ui.newstats.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The formatting that does not depend on a locale's date patterns. The date ranges live in
 * [StatsDateFormatterTest], which needs the real ICU data to assert them.
 */
class StatsFormatterTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        // formatStatValue groups digits with Locale.getDefault(). Pin the locale for determinism,
        // and restore it in tearDown so this global mutation doesn't leak into other tests.
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `toContentDescription of a single-line current-year range omits the separator`() {
        val label = DateRangeLabel(RangeLine.Single("14-20 Jun"), null)

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = false)).isEqualTo("14-20 Jun")
    }

    @Test
    fun `toContentDescription of a single-line past-year range merges the year`() {
        val label = DateRangeLabel(RangeLine.Single("14-20 Jun"), RangeLine.Single("2024"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = false))
            .isEqualTo("14-20 Jun 2024")
    }

    @Test
    fun `toContentDescription of a split current-year range spans both endpoints`() {
        val label = DateRangeLabel(RangeLine.Split("28 Jul", "3 Aug"), null)

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = false))
            .isEqualTo("28 Jul to 3 Aug")
    }

    @Test
    fun `toContentDescription of a split past-year range merges the year onto the end`() {
        val label = DateRangeLabel(RangeLine.Split("28 Jul", "3 Aug"), RangeLine.Single("2024"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = false))
            .isEqualTo("28 Jul to 3 Aug 2024")
    }

    @Test
    fun `toContentDescription of a cross-year range merges each year onto its endpoint`() {
        val label = DateRangeLabel(RangeLine.Split("14 Jun", "14 Jun"), RangeLine.Split("2025", "2026"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = false))
            .isEqualTo("14 Jun 2025 to 14 Jun 2026")
    }

    @Test
    fun `toContentDescription of a past-year range opens with the year where the year leads`() {
        // Japanese writes a date year-first ("2024年6月14日"). The two lines can ignore that — the
        // year has its own line — but the screen reader hears them as one string (CMM-2540).
        val label = DateRangeLabel(RangeLine.Split("6月14日", "6月20日"), RangeLine.Single("2024"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = true))
            .isEqualTo("2024 6月14日 to 6月20日")
    }

    @Test
    fun `toContentDescription of a cross-year range opens each end with its year where it leads`() {
        val label = DateRangeLabel(
            RangeLine.Split("12月30日", "1月2日"),
            RangeLine.Split("2025", "2026")
        )

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = true))
            .isEqualTo("2025 12月30日 to 2026 1月2日")
    }

    @Test
    fun `toContentDescription of a preset keeps its name ahead of the dates it resolved to`() {
        // Not a year line: the second line is the window the preset's name currently covers, so it
        // follows the name in every locale.
        val label = DateRangeLabel(RangeLine.Single("Last 7 days"), RangeLine.Single("21-27 Oct 2026"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE, yearLeads = true))
            .isEqualTo("Last 7 days 21-27 Oct 2026")
    }

    @Test
    fun `formatStatValue Long below 1000 returns raw number`() {
        assertThat(formatStatValue(500L)).isEqualTo("500")
    }

    @Test
    fun `formatStatValue Long zero returns zero`() {
        assertThat(formatStatValue(0L)).isEqualTo("0")
    }

    @Test
    fun `formatStatValue Long thousands returns K suffix`() {
        assertThat(formatStatValue(1500L)).isEqualTo("1.5K")
    }

    @Test
    fun `formatStatValue Long exact thousand returns K suffix`() {
        assertThat(formatStatValue(1000L)).isEqualTo("1.0K")
    }

    @Test
    fun `formatStatValue Long millions returns M suffix`() {
        assertThat(formatStatValue(2500000L)).isEqualTo("2.5M")
    }

    @Test
    fun `formatStatValue Long exact million returns M suffix`() {
        assertThat(formatStatValue(1000000L)).isEqualTo("1.0M")
    }

    @Test
    fun `formatStatValue Double whole number returns no decimals`() {
        assertThat(formatStatValue(5.0)).isEqualTo("5")
    }

    @Test
    fun `formatStatValue Double zero returns zero`() {
        assertThat(formatStatValue(0.0)).isEqualTo("0")
    }

    @Test
    fun `formatStatValue Double fractional returns one decimal`() {
        assertThat(formatStatValue(5.5)).isEqualTo("5.5")
    }

    @Test
    fun `formatStatValue Double negative value formats correctly`() {
        assertThat(formatStatValue(-3.7)).isEqualTo("-3.7")
    }

    @Test
    fun `formatStatValue Double large value formats correctly`() {
        assertThat(formatStatValue(1234.5)).isEqualTo("1234.5")
    }

    @Test
    fun `formatStatValue Double negative whole number returns no decimals`() {
        assertThat(formatStatValue(-2.0)).isEqualTo("-2")
    }

    companion object {
        // Mirrors R.string.stats_traffic_date_range_content_description ("%1$s to %2$s").
        private const val RANGE_TEMPLATE = "%1\$s to %2\$s"
    }
}
