package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithTwoValues

class ListItemWithTwoValuesViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_list_item_with_two_values
) {
    private val text = itemView.findViewById<TextView>(R.id.text)
    private val value1 = itemView.findViewById<TextView>(R.id.value1)
    private val value2 = itemView.findViewById<TextView>(R.id.value2)

    fun bind(item: ListItemWithTwoValues) {
        text.text = item.text
        value1.text = item.value1
        value2.text = item.value2
        itemView.contentDescription = item.contentDescription
    }
}
