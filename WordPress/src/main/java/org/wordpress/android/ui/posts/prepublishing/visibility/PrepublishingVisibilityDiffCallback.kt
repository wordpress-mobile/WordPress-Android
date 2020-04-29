package org.wordpress.android.ui.posts.prepublishing.visibility

import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.VisibilityUiState
import androidx.recyclerview.widget.DiffUtil.Callback

class PrepublishingVisibilityDiffCallback(
    private val oldList: List<VisibilityUiState>,
    private val newList: List<VisibilityUiState>
) : Callback() {
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
