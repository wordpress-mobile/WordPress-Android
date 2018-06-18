package org.wordpress.android.ui.activitylog.list

import android.support.v7.util.DiffUtil

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogListItem>,
    private val newList: List<ActivityLogListItem>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is ActivityLogListItem.Event && newItem is ActivityLogListItem.Event) {
            oldItem.activityId == newItem.activityId
        } else {
            false
        }
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
