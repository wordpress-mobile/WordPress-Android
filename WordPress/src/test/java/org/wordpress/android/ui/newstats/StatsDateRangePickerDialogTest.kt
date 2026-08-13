package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

/**
 * Material3's date picker works entirely in UTC start-of-day millis, so these conversions must not
 * depend on the device zone. See CMM-2271: reading them locally shifted the applied range back a
 * day west of UTC, and left today unselectable east of it.
 */
class StatsDateRangePickerDialogTest {
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `when reading the picker selection, then the tapped day survives every device zone`() {
        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            assertThat(TEST_DATE_UTC_MILLIS.toPickerDate())
                .describedAs("selection read in %s", zone)
                .isEqualTo(TEST_DATE)
        }
    }

    @Test
    fun `when converting a date for the picker, then it lands on UTC midnight in every device zone`() {
        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            assertThat(TEST_DATE.toPickerMillis())
                .describedAs("date converted in %s", zone)
                .isEqualTo(TEST_DATE_UTC_MILLIS)
        }
    }

    @Test
    fun `when guarding future dates, then today's cell matches the picker frame in every zone`() {
        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            // The guard in StatsDateRangePickerDialog. The expected cell is derived independently
            // from epoch days, so this fails if todayMillis ever drifts back to local midnight.
            val today = LocalDate.now()

            assertThat(today.toPickerMillis())
                .describedAs("today's cell in %s", zone)
                .isEqualTo(today.toEpochDay() * MILLIS_PER_DAY)
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

        private val TEST_DATE: LocalDate = LocalDate.of(2026, 8, 2)
        private val TEST_DATE_UTC_MILLIS: Long = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()

        // Spans both sides of UTC, including the half-hour offset of Asia/Kolkata.
        private val ZONES = listOf(
            "America/Los_Angeles",
            "America/New_York",
            "UTC",
            "Europe/Madrid",
            "Asia/Kolkata",
            "Asia/Tokyo",
            "Pacific/Auckland"
        )
    }
}
