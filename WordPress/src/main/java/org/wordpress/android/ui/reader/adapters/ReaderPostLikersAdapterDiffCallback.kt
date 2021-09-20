package org.wordpress.android.ui.reader.adapters

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.FaceItem

class ReaderPostLikersAdapterDiffCallback(
    private val oldItems: List<TrainOfFacesItem>,
    private val newItems: List<TrainOfFacesItem>
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
            oldItem is FaceItem && newItem is FaceItem -> oldItem.userId == newItem.userId
            oldItem is BloggersLikingTextItem && newItem is BloggersLikingTextItem -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
