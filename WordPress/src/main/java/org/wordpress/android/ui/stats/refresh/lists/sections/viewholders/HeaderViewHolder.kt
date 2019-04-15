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
    private val startLabel = itemView.findViewById<TextView>(id.start_label)
    private val endLabel = itemView.findViewById<TextView>(id.end_label)
    fun bind(item: Header) {
        startLabel.setText(item.startLabel)
        endLabel.setText(item.endLabel)
    }
}
