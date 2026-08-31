package org.wordpress.android.ui.newstats.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.util.Locale

class StatsFormatterTest {
    private val resourceProvider = mock<ResourceProvider>()
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        // toDateRangeString formats months with Locale.getDefault(), so the "Jul"/"Aug"/... month
        // abbreviations these tests assert are locale-sensitive. Pin the locale for determinism, and
        // restore it in tearDown so this global mutation doesn't leak into other tests in the runner.
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `toDateRangeString for a single-day range renders one date`() {
        val period = StatsPeriod.Custom(LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 11))

        assertThat(period.toDateRangeString(resourceProvider)).isEqualTo("11 Aug")
    }

    @Test
    fun `toDateRangeString within one month renders shared month`() {
        val period = StatsPeriod.Custom(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 30))

        assertThat(period.toDateRangeString(resourceProvider)).isEqualTo("28-30 Jul")
    }

    @Test
    fun `toDateRangeString across a month boundary renders month on both ends`() {
        val period = StatsPeriod.Custom(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 3))

        assertThat(period.toDateRangeString(resourceProvider)).isEqualTo("28 Jul - 3 Aug")
    }

    @Test
    fun `toDateRangeString across a year boundary renders month on both ends`() {
        val period = StatsPeriod.Custom(LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2))

        assertThat(period.toDateRangeString(resourceProvider)).isEqualTo("30 Dec - 2 Jan")
    }

    @Test
    fun `formatCustomDateRangeTwoLine hides the year for a single day in the current year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 6, 14), LocalDate.of(2025, 6, 14), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Single("14 Jun"))
        assertThat(label.secondary).isNull()
    }

    @Test
    fun `formatCustomDateRangeTwoLine shows the year for a single day in a past year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2024, 6, 14), LocalDate.of(2024, 6, 14), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Single("14 Jun"))
        assertThat(label.secondary).isEqualTo(RangeLine.Single("2024"))
    }

    @Test
    fun `formatCustomDateRangeTwoLine hides the year within one month of the current year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 6, 14), LocalDate.of(2025, 6, 20), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Single("14-20 Jun"))
        assertThat(label.secondary).isNull()
    }

    @Test
    fun `formatCustomDateRangeTwoLine shows the year within one month of a past year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2024, 6, 14), LocalDate.of(2024, 6, 20), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Single("14-20 Jun"))
        assertThat(label.secondary).isEqualTo(RangeLine.Single("2024"))
    }

    @Test
    fun `formatCustomDateRangeTwoLine hides the year across a month boundary of the current year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 7, 28), LocalDate.of(2025, 8, 3), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Split("28 Jul", "3 Aug"))
        assertThat(label.secondary).isNull()
    }

    @Test
    fun `formatCustomDateRangeTwoLine shows the year across a month boundary of a past year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2024, 7, 28), LocalDate.of(2024, 8, 3), currentYear = 2025
        )

        assertThat(label.primary).isEqualTo(RangeLine.Split("28 Jul", "3 Aug"))
        assertThat(label.secondary).isEqualTo(RangeLine.Single("2024"))
    }

    @Test
    fun `formatCustomDateRangeTwoLine across years splits both lines even when one is the current year`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 6, 14), LocalDate.of(2026, 6, 14), currentYear = 2026
        )

        assertThat(label.primary).isEqualTo(RangeLine.Split("14 Jun", "14 Jun"))
        assertThat(label.secondary).isEqualTo(RangeLine.Split("2025", "2026"))
    }

    @Test
    fun `formatCustomDateRangeTwoLine across a year boundary splits both lines`() {
        val label = formatCustomDateRangeTwoLine(
            LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2), currentYear = 2026
        )

        assertThat(label.primary).isEqualTo(RangeLine.Split("30 Dec", "2 Jan"))
        assertThat(label.secondary).isEqualTo(RangeLine.Split("2025", "2026"))
    }

    @Test
    fun `toContentDescription of a single-line current-year range omits the separator`() {
        val label = DateRangeLabel(RangeLine.Single("14-20 Jun"), null)

        assertThat(label.toContentDescription(RANGE_TEMPLATE)).isEqualTo("14-20 Jun")
    }

    @Test
    fun `toContentDescription of a single-line past-year range merges the year`() {
        val label = DateRangeLabel(RangeLine.Single("14-20 Jun"), RangeLine.Single("2024"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE)).isEqualTo("14-20 Jun 2024")
    }

    @Test
    fun `toContentDescription of a split current-year range spans both endpoints`() {
        val label = DateRangeLabel(RangeLine.Split("28 Jul", "3 Aug"), null)

        assertThat(label.toContentDescription(RANGE_TEMPLATE)).isEqualTo("28 Jul to 3 Aug")
    }

    @Test
    fun `toContentDescription of a split past-year range merges the year onto the end`() {
        val label = DateRangeLabel(RangeLine.Split("28 Jul", "3 Aug"), RangeLine.Single("2024"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE)).isEqualTo("28 Jul to 3 Aug 2024")
    }

    @Test
    fun `toContentDescription of a cross-year range merges each year onto its endpoint`() {
        val label = DateRangeLabel(RangeLine.Split("14 Jun", "14 Jun"), RangeLine.Split("2025", "2026"))

        assertThat(label.toContentDescription(RANGE_TEMPLATE)).isEqualTo("14 Jun 2025 to 14 Jun 2026")
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
