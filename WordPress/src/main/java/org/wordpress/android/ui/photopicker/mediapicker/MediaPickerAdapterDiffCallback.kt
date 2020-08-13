package org.wordpress.android.ui.photopicker.mediapicker

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerAdapterDiffCallback.Payload.COUNT_CHANGE
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerAdapterDiffCallback.Payload.SELECTION_CHANGE

class MediaPickerAdapterDiffCallback(
    private val oldItems: List<MediaPickerUiItem>,
    private val updatedItems: List<MediaPickerUiItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return updatedItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].id == updatedItems[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == updatedItems[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val updatedItem = updatedItems[newItemPosition]
        if (oldItem.isSelected != updatedItem.isSelected) {
            return SELECTION_CHANGE
        }
        if (oldItem.showOrderCounter == updatedItem.showOrderCounter &&
                oldItem.selectedOrder != updatedItem.selectedOrder) {
            return COUNT_CHANGE
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

    enum class Payload {
        SELECTION_CHANGE, COUNT_CHANGE
    }
}
