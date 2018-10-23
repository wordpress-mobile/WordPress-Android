package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.util.DiffUtil
import org.wordpress.android.ui.history.HistoryListItem.Revision

class HistoryDiffCallback(
    private val oldList: List<HistoryListItem>,
    private val newList: List<HistoryListItem>
) : DiffUtil.Callback() {
    companion object {
        const val AVATAR_CHANGED_KEY = "avatar_changed"
    }

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

    // currently only thing that can change in Revision is avatar and display name of author
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        if (oldItem is Revision && newItem is Revision) {
            if (oldItem.authorAvatarURL != newItem.authorAvatarURL) {
                val diffBundle = Bundle()
                diffBundle.putString(AVATAR_CHANGED_KEY, newItem.authorAvatarURL)
                return diffBundle
            }
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
