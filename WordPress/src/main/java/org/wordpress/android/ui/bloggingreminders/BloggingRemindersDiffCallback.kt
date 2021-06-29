package org.wordpress.android.ui.bloggingreminders

import androidx.recyclerview.widget.DiffUtil.Callback

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
}
