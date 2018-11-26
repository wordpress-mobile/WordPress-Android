package org.wordpress.android.ui.stats.refresh

import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Locale

class StatsDateFormatterTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var statsDateFormatter: StatsDateFormatter
    @Before
    fun setUp() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        statsDateFormatter = StatsDateFormatter(localeManagerWrapper)
    }

    @Test
    fun `parses a date`() {
        val unparsedDate = "2018-11-25"

        val parsedDate = statsDateFormatter.parseDate(unparsedDate)

        assertThat(parsedDate).isEqualTo("Nov 25, 2018")
    }
}
