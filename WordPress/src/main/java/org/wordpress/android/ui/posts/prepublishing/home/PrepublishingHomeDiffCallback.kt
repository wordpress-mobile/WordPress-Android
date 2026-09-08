package org.wordpress.android.ui.posts.prepublishing.home

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.FeaturedImageUiState

class PrepublishingHomeDiffCallback(
    private val oldList: List<PrepublishingHomeItemUiState>,
    private val newList: List<PrepublishingHomeItemUiState>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]

        // The featured-image card refreshes often (upload progress, post changes) with fresh
        // callback instances, so treat it as the same item to rebind in place rather than
        // remove+insert, which reloads the image and flickers.
        if (oldItem is FeaturedImageUiState && newItem is FeaturedImageUiState) return true

        return (oldItem == newItem)
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // Compare only the rendered content, ignoring the (always-changing) callback instances, so
        // an unchanged featured-image state doesn't trigger an unnecessary rebind.
        if (oldItem is FeaturedImageUiState.Visible && newItem is FeaturedImageUiState.Visible) {
            return oldItem.featuredImageData == newItem.featuredImageData
        }

        return oldItem == newItem
    }
}
