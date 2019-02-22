package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header

class HeaderViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_header_item
) {
    private val leftLabel = itemView.findViewById<TextView>(id.left_label)
    private val rightLabel = itemView.findViewById<TextView>(id.right_label)
    fun bind(item: Header) {
        leftLabel.setText(item.leftLabel)
        rightLabel.setText(item.rightLabel)
    }
}
