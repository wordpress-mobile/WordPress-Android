package org.wordpress.android.ui.comments

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

class CommentSubHeaderViewHolder(parent: ViewGroup) : CommentListViewHolder(
        parent,
        R.layout.comment_list_subheader
) {
    private val header: TextView = itemView.findViewById(R.id.label)

    fun bind(item: CommentListItem.SubHeader) {
        header.text = item.label
    }
}
