package org.wordpress.android.ui.activitylog.list

import android.support.v7.util.DiffUtil

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogListItem>,
    private val newList: List<ActivityLogListItem>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition] as? ActivityLogListItem.Event
        val newItem = newList[newItemPosition] as? ActivityLogListItem.Event
        return if (oldItem != null && newItem != null) {
            oldItem.activityId == newItem.activityId
        } else {
            oldItem == newItem
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
