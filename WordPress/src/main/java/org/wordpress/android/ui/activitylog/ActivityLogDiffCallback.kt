package org.wordpress.android.ui.activitylog

import android.support.v7.util.DiffUtil
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogListItemViewModel>,
    private val newList: List<ActivityLogListItemViewModel>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].activityId == newList[newItemPosition].activityId
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
