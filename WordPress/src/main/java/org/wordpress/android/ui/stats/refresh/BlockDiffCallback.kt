package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback
import android.util.Log

class BlockDiffCallback(
    private val oldList: List<BlockListItem>,
    private val newList: List<BlockListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        val result = oldItem.itemId == newItem.itemId
        Log.d("list_insights_block_dif", "ID: Old item: type: ${oldItem.itemId}")
        Log.d("list_insights_block_dif", "ID: New item: type: ${newItem.itemId}")
        Log.d("list_insights_block_dif", "areContentsTheSame: $result")
        return result
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        val result = oldList[oldItemPosition] == newList[newItemPosition]
        Log.d("list_insights_block_dif", "C: Old item: type: ${oldItem.itemId}")
        Log.d("list_insights_block_dif", "C: New item: type: ${newItem.itemId}")
        Log.d("list_insights_block_dif", "areContentsTheSame: $result")
        return result
    }
}
