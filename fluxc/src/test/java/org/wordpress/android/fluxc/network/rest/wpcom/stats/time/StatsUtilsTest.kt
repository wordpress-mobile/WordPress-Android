package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Calendar
import java.util.Locale

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
        val cal = Calendar.getInstance(Locale.ROOT)
        cal.set(2018, 10, 10, 23, 55)

        val result = statsUtils.getFormattedDate(cal.time)

        assertThat(result).isEqualTo("2018-11-10")
    }

    @Test
    fun `keeps correct date when timezone is within bounds`() {
        val cal = Calendar.getInstance(Locale.ROOT)
        cal.set(2018, 10, 10, 0, 15)

        val result = statsUtils.getFormattedDate(cal.time)

        assertThat(result).isEqualTo("2018-11-10")
    }

    @Test
    fun `moves the date forward when the site timezone is different`() {
        val cal = Calendar.getInstance(Locale.UK)
        cal.set(2018, 10, 10, 23, 55)

        val timeZone = SiteUtils.getNormalizedTimezone("+5")
        val result = statsUtils.getFormattedDate(cal.time, timeZone)

        assertThat(result).isEqualTo("2018-11-11")
    }

    @Test
    fun `moves the date back when the site timezone is different`() {
        val cal = Calendar.getInstance(Locale.UK)
        cal.set(2018, 10, 10, 0, 15)

        val timeZone = SiteUtils.getNormalizedTimezone("-5")
        val result = statsUtils.getFormattedDate(cal.time, timeZone)

        assertThat(result).isEqualTo("2018-11-09")
    }
}
