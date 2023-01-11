package org.wordpress.android.workers.reminder

import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.DAILY
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.WEEKLY
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.next

sealed class ReminderConfig(val type: ReminderType) {
    object DailyReminder : ReminderConfig(DAILY)
    data class WeeklyReminder(val days: Set<DayOfWeek>) : ReminderConfig(WEEKLY) {
        init {
            require(days.isNotEmpty()) {
                "Set of days can't be empty!"
            }
        }
    }

    enum class ReminderType {
        DAILY, WEEKLY
    }

    fun calculateNext(from: LocalDate = LocalDate.now()): LocalDate = when (this) {
        is DailyReminder -> from.plusDays(1)
        is WeeklyReminder -> from.withNextDayOfWeekFrom(days)!! // We know the set won't be empty
    }

    private fun LocalDate.withNextDayOfWeekFrom(days: Set<DayOfWeek>) = days.map { with(next(it)) }.minOrNull()

    fun toMap() = mapOf(
        REMINDER_TYPE to type.name,
        REMINDER_DAYS to when (this) {
            is DailyReminder -> null
            is WeeklyReminder -> days.joinToString(DAYS_SEPARATOR)
        }
    )

    companion object {
        private const val REMINDER_TYPE = "reminder_type"
        private const val REMINDER_DAYS = "reminder_days"
        private const val DAYS_SEPARATOR = ","

        fun fromMap(map: Map<String, Any?>): ReminderConfig {
            val type: ReminderType = (map[REMINDER_TYPE] as? String)
                ?.let {
                    runCatching { ReminderType.valueOf(it) }.getOrNull()
                }
                ?: DAILY
            val days: Set<DayOfWeek> = (map[REMINDER_DAYS] as? String)
                ?.split(DAYS_SEPARATOR)
                ?.mapNotNull {
                    runCatching { DayOfWeek.valueOf(it) }.getOrNull()
                }
                ?.toSet()
                .orEmpty()
            return when (type) {
                DAILY -> DailyReminder
                WEEKLY -> if (days.isEmpty()) DailyReminder else WeeklyReminder(days)
            }
        }
    }
}
