package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.IActionableItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Notice
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Progress

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogListItem>,
    private val newList: List<ActivityLogListItem>
) : DiffUtil.Callback() {
    companion object {
        const val LIST_ITEM_BUTTON_VISIBILITY_KEY = "list_item_button_visibility_key"
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is Event && newItem is Event -> oldItem.activityId == newItem.activityId
            oldItem is Progress && newItem is Progress -> oldItem == newItem
            oldItem is Notice && newItem is Notice -> oldItem == newItem
            else -> false
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

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        val bundle = Bundle()
        if (oldItem is IActionableItem && newItem is IActionableItem &&
            oldItem.isButtonVisible != newItem.isButtonVisible
        ) {
            bundle.putBoolean(LIST_ITEM_BUTTON_VISIBILITY_KEY, newItem.isButtonVisible)
        }

        if (bundle.size() == 0) return null
        return bundle
    }
}
