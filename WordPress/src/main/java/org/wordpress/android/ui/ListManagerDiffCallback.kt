package org.wordpress.android.ui

import android.support.v7.util.DiffUtil
import org.wordpress.android.fluxc.model.list.ListManager

class ListManagerDiffCallback<T>(
    private val oldListManager: ListManager<T>?,
    private val newListManager: ListManager<T>,
    private val areItemsTheSame: (T, T) -> Boolean,
    private val areContentsTheSame: (T, T) -> Boolean
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldListManager == null) {
            return false
        }
        val oldItem = oldListManager.getItem(
                position = oldItemPosition,
                shouldFetchIfNull = false,
                shouldLoadMoreIfNecessary = false
        )
        val newItem = newListManager.getItem(
                position = newItemPosition,
                shouldFetchIfNull = false,
                shouldLoadMoreIfNecessary = false
        )
        if (oldItem == null && newItem == null) {
            return true
        }
        if (oldItem == null || newItem == null) {
            return false
        }
        return areItemsTheSame(oldItem, newItem)
    }

    override fun getOldListSize(): Int {
        return oldListManager?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return newListManager.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldListManager?.getItem(
                position = oldItemPosition,
                shouldFetchIfNull = false,
                shouldLoadMoreIfNecessary = false
        )
        val newItem = newListManager.getItem(
                position = newItemPosition,
                shouldFetchIfNull = false,
                shouldLoadMoreIfNecessary = false
        )
        if (oldItem == null && newItem == null) {
            return true
        }
        if (oldItem == null || newItem == null) {
            return false
        }
        return areContentsTheSame(oldItem, newItem)
    }
}
