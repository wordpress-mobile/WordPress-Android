package org.wordpress.android.ui.mlp

import androidx.recyclerview.widget.DiffUtil.Callback

/**
 * Implements the Recyclerview list items diff to avoid unneeded UI refresh
 */
class ModalLayoutPickerListDiffCallback(
    private val oldList: List<ModalLayoutPickerListItem>,
    private val newList: List<ModalLayoutPickerListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem == newItem
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
}
