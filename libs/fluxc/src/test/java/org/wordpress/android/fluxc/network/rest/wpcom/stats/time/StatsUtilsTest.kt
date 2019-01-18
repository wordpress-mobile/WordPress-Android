package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class StatsUtilsTest {
    @Mock lateinit var siteModel: SiteModel
    @Mock lateinit var currentDateUtils: CurrentDateUtils
    private lateinit var statsUtils: StatsUtils
    @Before
    fun setUp() {
        statsUtils = StatsUtils(currentDateUtils)
    }

    @Test
    fun `moves date to future when timezone is adds time`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.set(2018, 10, 10, 23, 55)
        whenever(currentDateUtils.getCurrentDate()).thenReturn(cal.time)
        whenever(siteModel.timezone).thenReturn("+1.5")

        val result = statsUtils.getFormattedDate(siteModel)

        assertThat(result).isEqualTo("2018-11-11")
    }

    @Test
    fun `keeps correct date when timezone is within bounds`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.set(2018, 10, 10, 0, 15)
        whenever(currentDateUtils.getCurrentDate()).thenReturn(cal.time)
        whenever(siteModel.timezone).thenReturn("+0.0")

        val result = statsUtils.getFormattedDate(siteModel)

        assertThat(result).isEqualTo("2018-11-10")
    }

    @Test
    fun `keeps correct date when timezone is null`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.set(2018, 10, 10, 0, 15)
        whenever(currentDateUtils.getCurrentDate()).thenReturn(cal.time)
        whenever(siteModel.timezone).thenReturn(null)

        val result = statsUtils.getFormattedDate(siteModel)

        assertThat(result).isEqualTo("2018-11-10")
    }

    @Test
    fun `moves date to past when timezone is removes time`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.set(2018, 10, 10, 0, 15)
        whenever(currentDateUtils.getCurrentDate()).thenReturn(cal.time)
        whenever(siteModel.timezone).thenReturn("-0.5")

        val result = statsUtils.getFormattedDate(siteModel)

        assertThat(result).isEqualTo("2018-11-09")
    }
}
