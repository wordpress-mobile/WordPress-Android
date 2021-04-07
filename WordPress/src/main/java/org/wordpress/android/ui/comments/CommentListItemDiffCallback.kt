package org.wordpress.android.ui.comments

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.comments.CommentListItem.Comment
import org.wordpress.android.ui.comments.CommentListItem.SubHeader

class CommentListItemDiffCallback(
    private val oldList: List<CommentListItem>,
    private val newList: List<CommentListItem>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is SubHeader && newItem is SubHeader -> oldItem.label == newItem.label
            oldItem is Comment && newItem is Comment -> {
                oldItem.comment.remoteCommentId == newItem.comment.remoteCommentId
            }
            else -> false
        }
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        if (oldItem is SubHeader && newItem is SubHeader) {
            return oldItem.label == newItem.label
        }

        if (oldItem is Comment && newItem is Comment) {
            return oldItem.comment.status == newItem.comment.status &&
                    oldItem.comment.content == newItem.comment.content &&
                    oldItem.comment.datePublished == newItem.comment.datePublished &&
                    oldItem.comment.authorName == newItem.comment.authorName
        }

        return false
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is Comment && newItem is Comment) {
            return Payload
        }
        return null
    }

    object Payload
}
