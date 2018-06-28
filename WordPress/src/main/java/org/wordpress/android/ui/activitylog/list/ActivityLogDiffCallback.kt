package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.support.v7.util.DiffUtil
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.IActionableItem

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogListItem>,
    private val newList: List<ActivityLogListItem>
) : DiffUtil.Callback() {
    companion object {
        const val LIST_ITEM_BUTTON_VISIBILITY_KEY = "list_item_button_visibility_key"
        const val LIST_ITEM_HEADER_VISIBILITY_KEY = "list_item_header_visibility_key"
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return if (oldItem.javaClass == newItem.javaClass) {
            when (oldItem) {
                is ActivityLogListItem.Event -> oldItem.activityId == (newItem as ActivityLogListItem.Event).activityId
                is ActivityLogListItem.Progress -> oldItem == newItem
            }
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

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        val bundle = Bundle()
        if (oldItem is IActionableItem && newItem is IActionableItem
                && oldItem.isButtonVisible != newItem.isButtonVisible) {
            bundle.putBoolean(LIST_ITEM_BUTTON_VISIBILITY_KEY, newItem.isButtonVisible)
        }

        if (oldItem.isHeaderVisible != newItem.isHeaderVisible) {
            bundle.putBoolean(LIST_ITEM_HEADER_VISIBILITY_KEY, newItem.isHeaderVisible)
        }
        return bundle
    }
}
