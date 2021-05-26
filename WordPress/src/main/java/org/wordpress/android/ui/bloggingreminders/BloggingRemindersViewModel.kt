package org.wordpress.android.ui.bloggingreminders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAdapter.Item
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.workers.reminder.ReminderConfig.DailyReminder
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.WEEKLY
import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import org.wordpress.android.workers.reminder.ReminderScheduler
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject

class BloggingRemindersViewModel
@Inject constructor(
    val scheduler: ReminderScheduler
) : ViewModel() {
    val items = scheduler.getAll().map { list -> list.map { Item(it) } }
    val message = MutableLiveData<Event<String>>()
    var type: ReminderType? = null
    var days: Set<DayOfWeek> = setOf()

    fun add(siteId: Long) {
        if (type == null) {
            message.value = Event("Select a type")
        } else if (type == WEEKLY && days.isEmpty()) {
            message.value = Event("Select at least one day")
        } else {
            val reminderConfig = when (type) {
                WEEKLY -> WeeklyReminder(days)
                else -> DailyReminder
            }
            val date = scheduler.schedule(siteId, reminderConfig)
            message.value = Event("Scheduled to $date")
        }
    }

    fun cancel(id: UUID) {
        scheduler.cancelById(id)
    }
}
