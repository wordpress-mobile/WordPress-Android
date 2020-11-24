package org.wordpress.android.ui.mysite

import androidx.recyclerview.widget.DiffUtil

class MySiteAdapterDiffCallback(
    private val oldItems: List<MySiteItem>,
    private val updatedItems: List<MySiteItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = updatedItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val updatedItem = updatedItems[newItemPosition]
        // TODO base this on unique identifier
        return oldItem.type == updatedItem.type && oldItem == updatedItem
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == updatedItems[newItemPosition]
    }
}
