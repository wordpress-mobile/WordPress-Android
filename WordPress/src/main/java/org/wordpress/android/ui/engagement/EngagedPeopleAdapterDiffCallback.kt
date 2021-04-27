package org.wordpress.android.ui.engagement

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngageItem.NextLikesPageLoader

class EngagedPeopleAdapterDiffCallback(
    private val oldItems: List<EngageItem>,
    private val newItems: List<EngageItem>
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
            oldItem is LikedItem && newItem is LikedItem -> oldItem.author == newItem.author
            oldItem is Liker && newItem is Liker -> oldItem.userId == newItem.userId
            oldItem is NextLikesPageLoader && newItem is NextLikesPageLoader -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
