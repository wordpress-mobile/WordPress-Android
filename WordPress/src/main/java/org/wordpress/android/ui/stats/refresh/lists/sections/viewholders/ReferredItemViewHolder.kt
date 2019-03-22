package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem

class ReferredItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_referred_item
) {
    private val label = itemView.findViewById<TextView>(id.label)
    private val title = itemView.findViewById<TextView>(id.title)
    fun bind(item: ReferredItem) {
        label.setText(item.label)
        title.text = item.itemTitle
    }
}
