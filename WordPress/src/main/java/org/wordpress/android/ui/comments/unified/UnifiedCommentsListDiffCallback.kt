package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader

class UnifiedCommentsListDiffCallback : DiffUtil.ItemCallback<UnifiedCommentListItem>() {
    override fun areItemsTheSame(oldItem: UnifiedCommentListItem, newItem: UnifiedCommentListItem): Boolean {
        return when {
            oldItem is SubHeader && newItem is SubHeader -> oldItem.id == newItem.id
            oldItem is Comment && newItem is Comment -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: UnifiedCommentListItem, newItem: UnifiedCommentListItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: UnifiedCommentListItem, newItem: UnifiedCommentListItem): Any {
        val bundle = Bundle()
        if (oldItem is Comment && newItem is Comment) {
            if (oldItem.isSelected != newItem.isSelected) {
                bundle.putBoolean(COMMENT_SELECTION_TOGGLED, newItem.isSelected)
            }

            if (oldItem.isPending != newItem.isPending) {
                bundle.putBoolean(COMMENT_PENDING_STATE_CHANGED, newItem.isPending)
            }
        }

        return bundle
    }

    companion object {
        const val COMMENT_SELECTION_TOGGLED = "COMMENT_SELECTION_TOGGLED"
        const val COMMENT_PENDING_STATE_CHANGED = "COMMENT_PENDING_STATE_CHANGED"
        const val COMMENT_CLICK_ACTION_CHANGED = "COMMENT_CLICK_ACTION_CHANGED"
        const val COMMENT_TOGGLE_ACTION_CHANGED = "COMMENT_TOGGLE_ACTION_CHANGED"
    }
}
