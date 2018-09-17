package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback

class StatsDiffCallback(
    private val oldList: List<StatsItem>,
    private val newList: List<StatsItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].uuid == newList[newItemPosition].uuid
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
