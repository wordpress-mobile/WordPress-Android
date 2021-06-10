package org.wordpress.android.ui.comments.unified

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
}
