package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListHeader

class ListHeaderViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_list_header_item
) {
    private val label = itemView.findViewById<TextView>(R.id.label)
    private val valueLabel1 = itemView.findViewById<TextView>(R.id.valueLabel1)
    private val valueLabel2 = itemView.findViewById<TextView>(R.id.valueLabel2)
    fun bind(item: ListHeader) {
        label.setText(item.label)
        valueLabel1.setText(item.valueLabel1)
        valueLabel2.setText(item.valueLabel2)
    }
}
