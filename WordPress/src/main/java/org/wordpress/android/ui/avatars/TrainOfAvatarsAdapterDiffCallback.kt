package org.wordpress.android.ui.avatars

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem

class TrainOfAvatarsAdapterDiffCallback(
    private val oldItems: List<TrainOfAvatarsItem>,
    private val newItems: List<TrainOfAvatarsItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem is AvatarItem && newItem is AvatarItem -> oldItem.userAvatarUrl == newItem.userAvatarUrl
            oldItem is TrailingLabelTextItem && newItem is TrailingLabelTextItem -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
