package org.wordpress.android.ui.comments.unified

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.comments.CommentListViewHolder
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader

class UnifiedCommentSubHeaderViewHolder(parent: ViewGroup) : CommentListViewHolder(
        parent,
        R.layout.comment_list_subheader
) {
    private val header: TextView = itemView.findViewById(R.id.label)

    fun bind(item: SubHeader) {
        header.text = item.label
    }
}
