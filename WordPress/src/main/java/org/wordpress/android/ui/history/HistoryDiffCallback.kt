package org.wordpress.android.ui.history

import android.support.v7.util.DiffUtil
import org.wordpress.android.ui.history.HistoryListItem.Revision

class HistoryDiffCallback(
    private val oldList: List<HistoryListItem>,
    private val newList: List<HistoryListItem>
) : DiffUtil.Callback() {
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is Revision && newItem is Revision -> oldItem.revisionId == newItem.revisionId
            else -> false
        }
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }
}
