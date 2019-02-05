package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import java.util.Calendar

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
        val cal = Calendar.getInstance()
        cal.set(2018, 10, 10, 23, 55)

        val result = statsUtils.getFormattedDate(cal.time)

        assertThat(result).isEqualTo("2018-11-10")
    }

    @Test
    fun `keeps correct date when timezone is within bounds`() {
        val cal = Calendar.getInstance()
        cal.set(2018, 10, 10, 0, 15)

        val result = statsUtils.getFormattedDate(cal.time)

        assertThat(result).isEqualTo("2018-11-10")
    }
}
