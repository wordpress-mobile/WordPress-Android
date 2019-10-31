package org.wordpress.android.ui.reader.subfilter.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.utils.UiHelpers

class SiteViewHolder(
    parent: ViewGroup
) : SubfilterListItemViewHolder(parent, R.layout.subfilter_list_item) {
    private val itemTitle = itemView.findViewById<TextView>(R.id.item_title)

    fun bind(site: Site, uiHelpers: UiHelpers) {
        super.bind(site, uiHelpers)
        this.itemTitle.text = uiHelpers.getTextOfUiString(parent.context, site.label)
    }
}
