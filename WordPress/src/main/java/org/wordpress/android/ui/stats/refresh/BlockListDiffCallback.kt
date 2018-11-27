package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

class BlockListDiffCallback(
    private val oldList: List<BlockListItem>,
    private val newList: List<BlockListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.type == newItem.type
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
