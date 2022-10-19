package org.wordpress.android.ui.photopicker

import androidx.recyclerview.widget.DiffUtil

@Suppress("DEPRECATION")
@Deprecated("This class is being refactored, if you implement any change, please also update " +
        "{@link org.wordpress.android.ui.mediapicker.MedaPickerAdapterDiffCallback}")
class PhotoPickerAdapterDiffCallback(
    private val oldItems: List<PhotoPickerUiItem>,
    private val updatedItems: List<PhotoPickerUiItem>
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
            return Payload.SELECTION_CHANGE
        }
        if (oldItem.showOrderCounter == updatedItem.showOrderCounter &&
                oldItem.selectedOrder != updatedItem.selectedOrder) {
            return Payload.COUNT_CHANGE
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

    enum class Payload {
        SELECTION_CHANGE, COUNT_CHANGE
    }
}
