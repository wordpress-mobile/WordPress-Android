package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title

class TitleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_title_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    private val menu = itemView.findViewById<ImageButton>(R.id.menu)
    fun bind(item: Title) {
        text.setTextOrHide(item.textResource, item.text)
        if (item.menuAction != null) {
            menu.visibility = View.VISIBLE
            menu.setOnClickListener { item.menuAction.invoke(menu) }
        } else {
            menu.visibility = View.GONE
            menu.setOnClickListener(null)
        }
    }
}
