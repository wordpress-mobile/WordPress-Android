package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class BloggingRemindersUiModel(
    val siteId: Int,
    val enabledDays: Set<DayOfWeek> = setOf(),
    val hour: Int,
    val minute: Int,
    val isPromptIncluded: Boolean,
    val isPromptsCardEnabled: Boolean,
) {
    fun getNotificationTime(): CharSequence =
        LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    fun getNotificationTime24hour(): CharSequence =
        LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))

    fun toReminderConfig() = WeeklyReminder(this.enabledDays)
}
