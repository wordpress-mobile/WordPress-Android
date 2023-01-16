package org.wordpress.android.workers.reminder

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.workers.reminder.ReminderConfig.DailyReminder
import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

@RunWith(MockitoJUnitRunner::class)
class ReminderConfigTest {
    @Test
    fun `creating weekly reminder with empty days throws exception`() {
        val emptyDays = emptySet<DayOfWeek>()
        assertThrows(IllegalArgumentException::class.java) {
            WeeklyReminder(emptyDays)
        }
    }

    @Test
    fun `toMap with daily reminder returns corresponding map`() {
        val expected = dailyMap
        val actual = DailyReminder.toMap()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `toMap with weekly reminder returns corresponding map`() {
        val expected = monWedFriMap
        val actual = WeeklyReminder(monWedFriSet).toMap()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with non existent type key defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(emptyMap())

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with null type defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(mapOf(REMINDER_TYPE to null))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with unknown type defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(mapOf(REMINDER_TYPE to "some_unknown_type"))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with valid daily map returns corresponding reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(dailyMap)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with valid weekly map returns corresponding reminder`() {
        val expected = WeeklyReminder(monWedFriSet)
        val actual = ReminderConfig.fromMap(monWedFriMap)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with weekly type and non existent days key defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(mapOf(REMINDER_TYPE to WEEKLY))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with weekly type and null days defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(
            mapOf(
                REMINDER_TYPE to WEEKLY,
                REMINDER_DAYS to null
            )
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with weekly type and empty days defaults to daily reminder`() {
        val expected = DailyReminder
        val actual = ReminderConfig.fromMap(
            mapOf(
                REMINDER_TYPE to WEEKLY,
                REMINDER_DAYS to ""
            )
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `fromMap with weekly type ignores invalid days`() {
        val expected = WeeklyReminder(setOf(MONDAY, WEDNESDAY))
        val actual = ReminderConfig.fromMap(
            mapOf(
                REMINDER_TYPE to WEEKLY,
                REMINDER_DAYS to "MONDAY,some_unknown_day,WEDNESDAY"
            )
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculateNext for daily reminder returns next day`() {
        val from = LocalDate.of(2021, 5, 15)
        val expected = LocalDate.of(2021, 5, 16)
        val actual = DailyReminder.calculateNext(from)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculateNext for weekly reminder returns next available day of week`() {
        val from = LocalDate.of(2021, 5, 15) // Saturday
        val expected = LocalDate.of(2021, 5, 17) // Monday
        val actual = WeeklyReminder(monWedFriSet).calculateNext(from)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculateNext for weekly reminder with same day returns next available day of week`() {
        val from = LocalDate.of(2021, 5, 17) // Monday
        val expected = LocalDate.of(2021, 5, 19) // Wednesday
        val actual = WeeklyReminder(monWedFriSet).calculateNext(from)

        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        private const val REMINDER_TYPE = "reminder_type"
        private const val REMINDER_DAYS = "reminder_days"
        private const val DAILY = "DAILY"
        private const val WEEKLY = "WEEKLY"
        private const val MON_WED_FRI = "MONDAY,WEDNESDAY,FRIDAY"

        private val dailyMap: Map<String, Any?> = mapOf(
            REMINDER_TYPE to DAILY,
            REMINDER_DAYS to null
        )

        private val monWedFriMap: Map<String, Any?> = mapOf(
            REMINDER_TYPE to WEEKLY,
            REMINDER_DAYS to MON_WED_FRI
        )

        private val monWedFriSet = setOf(MONDAY, WEDNESDAY, FRIDAY)
    }
}
