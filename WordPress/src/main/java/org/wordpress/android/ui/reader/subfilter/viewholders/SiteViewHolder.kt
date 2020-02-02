package org.wordpress.android.ui.reader.subfilter.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.UrlUtils

class SiteViewHolder(
    parent: ViewGroup
) : SubfilterListItemViewHolder(parent, R.layout.subfilter_list_item) {
    private val itemTitle = itemView.findViewById<TextView>(R.id.item_title)
    private val itemUrl = itemView.findViewById<TextView>(R.id.item_url)

    fun bind(site: Site, uiHelpers: UiHelpers) {
        super.bind(site, uiHelpers)
        this.itemTitle.text = uiHelpers.getTextOfUiString(parent.context, site.label)
        this.itemUrl.visibility = View.VISIBLE

        val blog = site.blog

        this.itemUrl.text = when {
            blog.hasUrl() -> UrlUtils.getHost(blog.url)
            blog.hasFeedUrl() -> UrlUtils.getHost(blog.feedUrl)
            else -> ""
        }
    }
}
