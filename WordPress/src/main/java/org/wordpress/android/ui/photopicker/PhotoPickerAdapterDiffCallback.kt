package org.wordpress.android.ui.photopicker

import android.util.Log
import androidx.recyclerview.widget.DiffUtil

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
        val areContentsTheSame = oldItems[oldItemPosition] == updatedItems[newItemPosition]
        if (!areContentsTheSame) {
            Log.d("vojta", "Contents are different")
            Log.d("vojta", "Item 1 selected: " + oldItems[oldItemPosition].isSelected)
            Log.d("vojta", "Item 2 selected: " + updatedItems[newItemPosition].isSelected)
        }
        return areContentsTheSame
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val (_, _, _, isSelected) = oldItems[oldItemPosition]
        val (id, _, _, isSelected1) = updatedItems[newItemPosition]
        Log.d("vojta", "Item 1 selected: $isSelected")
        Log.d("vojta", "Item 2 selected: $isSelected1")
        if (isSelected != isSelected1) {
            Log.d("vojta", "Sending payload")
            return id
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
