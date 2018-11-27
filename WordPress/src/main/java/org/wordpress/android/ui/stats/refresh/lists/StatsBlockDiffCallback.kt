package org.wordpress.android.ui.stats.refresh.lists

import android.support.v7.util.DiffUtil.Callback

class StatsBlockDiffCallback(
    private val oldList: List<StatsBlock>,
    private val newList: List<StatsBlock>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return when {
            oldItem.statsTypes != null && newItem.statsTypes != null -> oldItem.statsTypes == newItem.statsTypes
            else -> oldItem.type == newItem.type
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (oldItem is BlockList && newItem is BlockList) {
            return oldItem.items.size == newItem.items.size
        }
        return null
    }
}
