package org.wordpress.android.ui.bloggingreminders

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons

object BloggingRemindersDiffCallback : DiffUtil.ItemCallback<BloggingRemindersItem>() {
    override fun areItemsTheSame(oldItem: BloggingRemindersItem, newItem: BloggingRemindersItem): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: BloggingRemindersItem, newItem: BloggingRemindersItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: BloggingRemindersItem, newItem: BloggingRemindersItem): Any? {
        if (oldItem is DayButtons && newItem is DayButtons) {
            return DayButtonsPayload(oldItem.dayItems.mapIndexed { index, dayItem ->
                dayItem != newItem.dayItems[index]
            }.toList())
        }
        return super.getChangePayload(oldItem, newItem)
    }

    data class DayButtonsPayload(val changedDays: List<Boolean>)
}
