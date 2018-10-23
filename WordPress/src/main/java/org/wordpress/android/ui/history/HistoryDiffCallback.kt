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
        const val DISPLAY_NAME_CHANGED_KEY = "display_name_changed"
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

    // currently only things that can change in Revision are avatar and display name of author
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        if (oldItem is Revision && newItem is Revision) {
            val changesBundle = Bundle()
            if (oldItem.authorAvatarURL != newItem.authorAvatarURL) {
                changesBundle.putString(AVATAR_CHANGED_KEY, newItem.authorAvatarURL)
            }
            if (oldItem.authorDisplayName != newItem.authorDisplayName) {
                changesBundle.putString(DISPLAY_NAME_CHANGED_KEY, newItem.authorDisplayName)
            }
            if (changesBundle.keySet().size > 0) {
                return changesBundle
            }
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
