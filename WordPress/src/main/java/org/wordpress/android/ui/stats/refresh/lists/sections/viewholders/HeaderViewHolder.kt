package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header

class HeaderViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_header_item
) {
    private val startLabel = itemView.findViewById<TextView>(R.id.start_label)
    private val endLabel = itemView.findViewById<TextView>(R.id.end_label)
    fun bind(item: Header) {
        startLabel.setText(item.startLabel)
        endLabel.setText(item.endLabel)
    }
}
