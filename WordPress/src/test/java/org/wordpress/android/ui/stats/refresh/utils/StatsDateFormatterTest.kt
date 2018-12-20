package org.wordpress.android.ui.stats.refresh.utils

import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

class StatsDateFormatterTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var statsDateFormatter: StatsDateFormatter
    @Before
    fun setUp() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        statsDateFormatter = StatsDateFormatter(localeManagerWrapper, resourceProvider)
    }

    @Test
    fun `parses a date`() {
        val unparsedDate = "2018-11-25"

        val parsedDate = statsDateFormatter.printDate(unparsedDate)

        assertThat(parsedDate).isEqualTo("Nov 25, 2018")
    }

    @Test
    fun `prints a day date`() {
        val unparsedDate = "2018-11-25"

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, DAYS)

        assertThat(parsedDate).isEqualTo("Nov 25, 2018")
    }

    @Test
    fun `prints a week date`() {
        val unparsedDate = "2018W12W19"
        val result = "Dec 17 - Dec 23"
        whenever(
                resourceProvider.getString(
                        R.string.stats_from_to_dates_in_week_label,
                        "Dec 17",
                        "Dec 23"
                )
        ).thenReturn(result)

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, WEEKS)

        assertThat(parsedDate).isEqualTo("Dec 17 - Dec 23")
    }

    @Test
    fun `prints a week date with year when week overlaps years`() {
        val unparsedDate = "2011W12W31"
        val result = "Dec 26, 2011 - Jan 1, 2012"
        whenever(
                resourceProvider.getString(
                        R.string.stats_from_to_dates_in_week_label,
                        "Dec 26, 2011",
                        "Jan 1, 2012"
                )
        ).thenReturn(result)

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, WEEKS)

        assertThat(parsedDate).isEqualTo(result)
    }

    @Test
    fun `prints a month date`() {
        val unparsedDate = "2018-12"

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, MONTHS)

        assertThat(parsedDate).isEqualTo("Dec, 2018")
    }

    @Test
    fun `prints a year date`() {
        val unparsedDate = "2018-12-01"

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, YEARS)

        assertThat(parsedDate).isEqualTo("2018")
    }

    @Test
    fun `parses a date in another language`() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.forLanguageTag("cs"))
        val unparsedDate = "2018-11-25"

        val parsedDate = statsDateFormatter.printDate(unparsedDate)

        assertThat(parsedDate).isEqualTo("25.11.2018")
    }

    @Test
    fun `prints a day date in another language`() {
        val unparsedDate = "2018-11-25"
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.forLanguageTag("cs"))

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, DAYS)

        assertThat(parsedDate).isEqualTo("25.11.2018")
    }

    @Test
    fun `prints a week date in another language`() {
        val result = "17.12 - 23.12"
        whenever(
                resourceProvider.getString(
                        R.string.stats_from_to_dates_in_week_label,
                        "17.12",
                        "23.12"
                )
        ).thenReturn(result)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.forLanguageTag("cs"))
        val unparsedDate = "2018W12W19"

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, WEEKS)

        assertThat(parsedDate).isEqualTo(result)
    }

    @Test
    fun `prints a month date in another language`() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.forLanguageTag("cs"))
        val unparsedDate = "2018-12"

        val parsedDate = statsDateFormatter.printGranularDate(unparsedDate, MONTHS)

        assertThat(parsedDate).isEqualTo("Pro, 2018")
    }
}
