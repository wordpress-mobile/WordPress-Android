package org.wordpress.android.ui.posts.prepublishing.home

import androidx.recyclerview.widget.DiffUtil

class PrepublishingHomeDiffCallback(
    private val oldList: List<PrepublishingHomeItemUiState>,
    private val newList: List<PrepublishingHomeItemUiState>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]

        return (oldItem == newItem)
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
}
