package org.wordpress.android.ui.sitecreation.theme

import androidx.recyclerview.widget.DiffUtil.Callback

class LayoutsDiffCallback(
    private val oldList: List<LayoutGridItemUiState>,
    private val newList: List<LayoutGridItemUiState>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            newList[newItemPosition].slug == oldList[oldItemPosition].slug

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val new = newList[newItemPosition]
        val old = oldList[oldItemPosition]
        return new.selected == old.selected && new.preview == old.preview && new.title == old.title
    }
}
