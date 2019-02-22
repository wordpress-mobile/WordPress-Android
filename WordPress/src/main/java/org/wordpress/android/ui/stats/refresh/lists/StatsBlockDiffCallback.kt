package org.wordpress.android.ui.stats.refresh.lists

import android.support.v7.util.DiffUtil.Callback
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Success

class StatsBlockDiffCallback(
    private val oldList: List<StatsBlock>,
    private val newList: List<StatsBlock>
) : Callback() {
    object Payload
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.statsTypes == newItem.statsTypes
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    /**
     * This method gets called when the items are the same and the contents changed. In this case we want to send
     * the change payload for BlockList items which causes the Adapter to reuse the same ViewHolder and thus prevents
     * the blinking of the item being redrawed. Use this method if you want to manually only change a part of View.
     */
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (oldItem is Success && newItem is Success) {
            return Payload
        }
        return null
    }
}
