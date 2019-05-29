package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag

class TagViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_tag_item
) {
    private val tag = itemView.findViewById<TextView>(id.tag)
    fun bind(item: Tag) {
        tag.setText(item.textResource)
    }
}
