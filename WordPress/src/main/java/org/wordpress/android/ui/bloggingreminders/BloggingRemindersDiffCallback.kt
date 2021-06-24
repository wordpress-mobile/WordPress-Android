package org.wordpress.android.ui.bloggingreminders

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons

class BloggingRemindersDiffCallback(
    private val oldList: List<BloggingRemindersItem>,
    private val newList: List<BloggingRemindersItem>
) : Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is DayButtons && newItem is DayButtons) {
            return DayButtonsPayload(oldItem.dayItems.mapIndexed { index, dayItem ->
                dayItem != newItem.dayItems[index]
            }.toList())
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

    data class DayButtonsPayload(val changedDays: List<Boolean>)
}
