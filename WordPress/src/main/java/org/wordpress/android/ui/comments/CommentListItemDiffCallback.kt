package org.wordpress.android.ui.comments

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.comments.CommentListItem.Comment
import org.wordpress.android.ui.comments.CommentListItem.SubHeader

@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
class CommentListItemDiffCallback(
    private val oldList: List<CommentListItem>,
    private val newList: List<CommentListItem>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when {
            oldItem is SubHeader && newItem is SubHeader -> oldItem.id == newItem.id
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

        var isSame = false

        if (oldItem is SubHeader && newItem is SubHeader) {
            isSame = oldItem.label == newItem.label
        }

        if (oldItem is Comment && newItem is Comment) {
            isSame = oldItem.comment.status == newItem.comment.status &&
                    oldItem.comment.content == newItem.comment.content &&
                    oldItem.comment.datePublished == newItem.comment.datePublished &&
                    oldItem.comment.authorName == newItem.comment.authorName
        }

        return isSame
    }

    /**
     * Since we don't currently have a logic for updating only a part of the view, this method sends empty Payload
     * to avoid view blinking in RecyclerView.
     */
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
