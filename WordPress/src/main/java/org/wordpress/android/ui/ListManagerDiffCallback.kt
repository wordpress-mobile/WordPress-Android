package org.wordpress.android.ui

import android.support.v7.util.DiffUtil
import org.wordpress.android.fluxc.model.list.ListManager

/**
 * A helper class which can be used to compare two [ListManager]s.
 *
 * @param oldListManager The current [ListManager] to be used in comparison
 * @param newListManager The new [ListManager] that'll replace the old one
 * @param areItemsTheSame A compare function to be used to determine if two items refer to the same object. This is
 * not the function to compare the contents of the items. In most cases, this should compare the ids of two items.
 * @param areContentsTheSame A compare function to be used to determine if two items has the same contents. This
 * function will be triggered for items who return true for [areItemsTheSame] and actual content comparison should be
 * made depending on the view they are used in.
 */
class ListManagerDiffCallback<T>(
    private val oldListManager: ListManager<T>?,
    private val newListManager: ListManager<T>,
    private val areItemsTheSame: (T, T) -> Boolean,
    private val areContentsTheSame: (T, T) -> Boolean
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldListManager?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return newListManager.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldListManager == null) {
            return false
        }
        return compareItems(oldItemPosition, newItemPosition, areItemsTheSame)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return compareItems(oldItemPosition, newItemPosition, areContentsTheSame)
    }

    /**
     * A helper function to compare two items in the given positions given a compare function.
     */
    private fun compareItems(oldItemPosition: Int, newItemPosition: Int, compare: (T, T) -> Boolean): Boolean {
        // We shouldn't fetch items or load more pages prematurely when we are just trying to compare them
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
        // If two items are null, they are the same for our intends and purposes
        if (oldItem == null && newItem == null) {
            return true
        }
        // If one of the items is null, but the other one is not, they can't be the same item
        if (oldItem == null || newItem == null) {
            return false
        }
        return compare(oldItem, newItem)
    }
}
