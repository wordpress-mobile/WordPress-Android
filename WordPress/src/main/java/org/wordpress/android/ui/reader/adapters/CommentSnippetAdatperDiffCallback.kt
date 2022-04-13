package org.wordpress.android.ui.reader.adapters

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.BUTTON
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.COMMENT
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.LOADING
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.TEXT_MESSAGE
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState

class CommentSnippetAdatperDiffCallback(
    private val oldItems: List<CommentSnippetItemState>,
    private val newItems: List<CommentSnippetItemState>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return if (oldItem.type == newItem.type) {
            when (oldItem.type) {
                LOADING -> true
                COMMENT, BUTTON, TEXT_MESSAGE -> oldItem == newItem
            }
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem == newItem
    }
}
