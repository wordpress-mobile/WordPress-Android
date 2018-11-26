package org.wordpress.android.ui.stats.refresh.lists

import android.support.v7.util.DiffUtil.Callback

class StatsBlockDiffCallback(
    private val oldList: List<StatsBlock>,
    private val newList: List<StatsBlock>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem.statsTypes != null && newItem.statsTypes != null) {
            oldItem.statsTypes == newItem.statsTypes
        } else {
            oldItem.type == newItem.type
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
