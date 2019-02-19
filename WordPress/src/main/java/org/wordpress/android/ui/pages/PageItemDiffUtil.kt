package org.wordpress.android.ui.pages

import android.support.v7.util.DiffUtil.Callback
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage

class PageItemDiffUtil(val items: List<PageItem>, val result: List<PageItem>) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = items[oldItemPosition]
        val newItem = result[newItemPosition]
        return oldItem.type == newItem.type && when (oldItem) {
            is Page -> oldItem.id == (newItem as Page).id
            is ParentPage -> oldItem.id == (newItem as ParentPage).id
            else -> oldItem == newItem
        }
    }

    override fun getOldListSize(): Int = items.size

    override fun getNewListSize(): Int = result.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return items[oldItemPosition] == result[newItemPosition]
    }
}
