package org.wordpress.android.ui.mlp

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.LayoutCategory
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Title
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.CATEGORIES
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.LAYOUTS
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.SUBTITLE
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.TITLE

/**
 * Implements the Recyclerview list items diff to avoid unneeded UI refresh
 */
class ModalLayoutPickerListDiffCallback(
    private val oldList: List<ModalLayoutPickerListItem>,
    private val newList: List<ModalLayoutPickerListItem>
) : Callback() {
    object Payload

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (newItem.type == oldItem.type) {
            return when (newItem.type) {
                TITLE -> (newItem as? Title)?.visible == (oldItem as? Title)?.visible
                SUBTITLE -> true
                CATEGORIES -> true // for now
                LAYOUTS -> {
                    val newLayoutCategory = (newItem as? LayoutCategory)
                    val oldLayoutCategory = (oldItem as? LayoutCategory)
                    return newLayoutCategory?.description == oldLayoutCategory?.description &&
                            newLayoutCategory?.layouts?.size == oldLayoutCategory?.layouts?.size
                }
            }
        } else return false
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (newItem.type == LAYOUTS) {
            val newLayoutCategory = (newItem as? LayoutCategory)
            val oldLayoutCategory = (oldItem as? LayoutCategory)
            newLayoutCategory?.layouts == oldLayoutCategory?.layouts
        } else {
            newItem == oldItem
        }
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (oldItem.type == newItem.type) {
            return Payload
        }
        return null
    }
}
