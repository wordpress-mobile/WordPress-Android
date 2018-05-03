package org.wordpress.android.ui.activitylog

import android.support.v7.util.DiffUtil
import org.wordpress.android.fluxc.model.activity.ActivityLogModel

class ActivityLogDiffCallback(
    private val oldList: List<ActivityLogModel>,
    private val newList: List<ActivityLogModel>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].activityID == newList[newItemPosition].activityID
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].summary == newList[newItemPosition].summary &&
                oldList[oldItemPosition].text == newList[newItemPosition].text
    }
}
