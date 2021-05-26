package org.wordpress.android.workers.reminder

import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.DAILY
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.WEEKLY
import java.time.DayOfWeek

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
}
