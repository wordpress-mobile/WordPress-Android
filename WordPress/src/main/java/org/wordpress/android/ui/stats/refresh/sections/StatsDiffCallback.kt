package org.wordpress.android.ui.stats.refresh.sections

import android.support.v7.util.DiffUtil.Callback

class StatsDiffCallback(
    private val oldList: List<StatsListItem>,
    private val newList: List<StatsListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem.insightsType != null && newItem.insightsType != null) {
            oldItem.insightsType == newItem.insightsType
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
