package org.wordpress.android.ui.comments

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
class CommentSubHeaderViewHolder(parent: ViewGroup) : CommentListViewHolder(
        parent,
        R.layout.comment_list_subheader
) {
    private val header: TextView = itemView.findViewById(R.id.label)

    fun bind(item: CommentListItem.SubHeader) {
        header.text = item.label
    }
}
