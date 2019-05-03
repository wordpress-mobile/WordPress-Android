package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LinkButton

class LinkButtonViewHolder(val parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_link_button_item
) {
    private val text = itemView.findViewById<TextView>(id.text)

    fun bind(item: LinkButton) {
        text.setText(item.text)
        text.setOnClickListener { item.navigateAction.click() }
    }
}
