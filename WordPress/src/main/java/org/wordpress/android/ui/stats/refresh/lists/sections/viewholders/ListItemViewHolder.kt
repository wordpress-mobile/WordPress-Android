package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem

class ListItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_list_item
) {
    private val icon = itemView.findViewById<ImageView>(R.id.icon)
    private val text = itemView.findViewById<TextView>(R.id.text)
    private val value = itemView.findViewById<TextView>(R.id.value)
    private val divider = itemView.findViewById<View>(R.id.divider)

    fun bind(item: ListItem) {
        icon.visibility = View.GONE
        text.text = item.text
        value.text = item.value
        divider.visibility = if (item.showDivider) {
            View.VISIBLE
        } else {
            View.GONE
        }
        itemView.contentDescription = item.contentDescription
    }
}
