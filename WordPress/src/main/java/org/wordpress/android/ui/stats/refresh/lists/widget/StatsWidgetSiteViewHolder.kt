package org.wordpress.android.ui.stats.refresh.lists.widget

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.stats_widget_site_selector_item.view.*
import org.wordpress.android.R.drawable
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.SiteUiModel
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR

class StatsWidgetSiteViewHolder(parent: ViewGroup, val imageManager: ImageManager) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                layout.stats_widget_site_selector_item,
                parent,
                false
        )
) {
    fun bind(site: SiteUiModel) {
        if (site.iconUrl != null) {
            imageManager.load(itemView.site_icon, BLAVATAR, site.iconUrl)
        } else {
            imageManager.load(
                    itemView.site_icon,
                    drawable.ic_placeholder_blavatar_grey_lighten_20_40dp
            )
        }
        if (site.title != null) {
            itemView.site_title.text = site.title
        } else {
            itemView.site_title.setText(string.unknown)
        }
        if (site.url != null) {
            itemView.site_url.text = site.url
        } else {
            itemView.site_url.setText(string.unknown)
        }
        itemView.site_container.setOnClickListener {
            site.click()
        }
    }
}
