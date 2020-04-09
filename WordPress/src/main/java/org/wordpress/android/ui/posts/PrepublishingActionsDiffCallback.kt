package org.wordpress.android.ui.posts

import androidx.recyclerview.widget.DiffUtil.Callback

class PrepublishingActionsDiffCallback(
    private val oldList: List<PrepublishingActionListItem>,
    private val newList: List<PrepublishingActionListItem>
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
