package org.wordpress.android.ui.reader.subfilter.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.utils.UiHelpers

class TagViewHolder(
    parent: ViewGroup
) : SubfilterListItemViewHolder(parent, R.layout.subfilter_list_item) {
    private val itemTitle = itemView.findViewById<TextView>(R.id.item_title)
    private val itemUrl = itemView.findViewById<TextView>(R.id.item_url)

    fun bind(tag: Tag, uiHelpers: UiHelpers) {
        super.bind(tag, uiHelpers)
        this.itemTitle.text = uiHelpers.getTextOfUiString(parent.context, tag.label)
        this.itemUrl.visibility = View.GONE
    }
}
