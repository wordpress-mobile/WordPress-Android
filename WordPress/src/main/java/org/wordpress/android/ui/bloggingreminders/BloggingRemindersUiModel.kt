package org.wordpress.android.ui.bloggingreminders

import java.time.DayOfWeek

data class BloggingRemindersUiModel(
    val siteId: Int,
    val enabledDays: Set<DayOfWeek> = setOf(),
    val hour: Int,
    val minute: Int,
) {
    fun getNotificationTime(): CharSequence {
        val period = if (hour >= 12) "PM" else "AM"
        val hour = if (hour > 12) hour - 12 else hour
        return "$hour:$minute $period"
    }
}
