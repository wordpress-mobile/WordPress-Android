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
        val tappedCell = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()

        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            assertThat(tappedCell.toSelectedDate())
                .describedAs("selection read in %s", zone)
                .isEqualTo(LocalDate.of(2026, 8, 2))
        }
    }

    @Test
    fun `when converting a date for the picker, then it lands on UTC midnight in every device zone`() {
        val expected = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()

        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            assertThat(LocalDate.of(2026, 8, 2).toUtcMillis())
                .describedAs("date converted in %s", zone)
                .isEqualTo(expected)
        }
    }

    @Test
    fun `when guarding future dates, then today is selectable and tomorrow is not in every zone`() {
        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            // The guard in StatsDateRangePickerDialog. Cells are derived independently from epoch
            // days, so this fails if todayMillis ever drifts back to local midnight.
            val today = LocalDate.now()
            val todayMillis = today.toUtcMillis()

            assertThat(todayMillis)
                .describedAs("today's cell in %s", zone)
                .isEqualTo(today.toEpochDay() * MILLIS_PER_DAY)
            assertThat(today.plusDays(1).toEpochDay() * MILLIS_PER_DAY <= todayMillis)
                .describedAs("tomorrow selectable in %s", zone)
                .isFalse()
        }
    }

    @Test
    fun `when a selection round trips, then the date is unchanged`() {
        val date = LocalDate.of(2026, 8, 2)

        ZONES.forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))

            assertThat(date.toUtcMillis().toSelectedDate())
                .describedAs("round trip in %s", zone)
                .isEqualTo(date)
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

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
