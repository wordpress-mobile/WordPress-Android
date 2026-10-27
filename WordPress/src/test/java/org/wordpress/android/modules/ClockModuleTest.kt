package org.wordpress.android.modules

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.util.TimeZone

class ClockModuleTest {
    private lateinit var previousTimeZone: TimeZone

    @Before
    fun setUp() {
        previousTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun `given the default zone changes after the clock is created, then the clock follows it`() {
        TimeZone.setDefault(TimeZone.getTimeZone(WEST_OF_DATE_LINE))
        val clock = ClockModule().provideClock()

        // What Android does in a running process when the device time zone changes.
        TimeZone.setDefault(TimeZone.getTimeZone(EAST_OF_DATE_LINE))

        // The zones are 25 hours apart, so a clock still in the first one would be a calendar day off.
        assertThat(clock.zone).isEqualTo(ZoneId.of(EAST_OF_DATE_LINE))
        val instant = clock.instant()
        assertThat(instant.atZone(clock.zone).toLocalDate())
            .isNotEqualTo(instant.atZone(ZoneId.of(WEST_OF_DATE_LINE)).toLocalDate())
    }

    @Test
    fun `given a fixed zone is requested, then the clock uses that zone`() {
        val zone = ZoneId.of(EAST_OF_DATE_LINE)

        val clock = ClockModule().provideClock().withZone(zone)

        assertThat(clock.zone).isEqualTo(zone)
    }

    companion object {
        private const val WEST_OF_DATE_LINE = "Pacific/Pago_Pago" // UTC-11
        private const val EAST_OF_DATE_LINE = "Pacific/Kiritimati" // UTC+14
    }
}
