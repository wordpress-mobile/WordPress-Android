package org.wordpress.android.ui.bloggingreminders

import java.time.DayOfWeek

data class BloggingRemindersUiModel(
    val siteId: Int,
    val enabledDays: Set<DayOfWeek> = setOf(),
    val hour: Int,
    val minute: Int
) {
    fun getNotificationTime(): CharSequence {
        val period = if (hour >= HOUR_12) "PM" else "AM"
        val hour = if (hour > HOUR_12) hour - HOUR_12 else hour
        return "$hour:$minute $period"
    }

    companion object {
        const val HOUR_12 = 12
    }
}
