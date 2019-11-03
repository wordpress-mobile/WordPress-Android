package org.wordpress.android.ui.main

import androidx.recyclerview.widget.DiffUtil.Callback

class MainActionDiffCallback(
    private val oldList: List<MainActionListItem>,
    private val newList: List<MainActionListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]

        return (oldItem.actionType == newItem.actionType)
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
}
