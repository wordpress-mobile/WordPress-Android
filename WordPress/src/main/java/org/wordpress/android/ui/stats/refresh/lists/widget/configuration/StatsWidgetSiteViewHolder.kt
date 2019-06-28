package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.stats_widget_site_selector_item.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel.SiteUiModel
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR

class StatsWidgetSiteViewHolder(parent: ViewGroup, val imageManager: ImageManager) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.stats_widget_site_selector_item,
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
                    R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp
            )
        }
        if (site.title != null) {
            itemView.site_title.text = site.title
        } else {
            itemView.site_title.setText(R.string.unknown)
        }
        if (site.url != null) {
            itemView.site_url.text = site.url
        } else {
            itemView.site_url.setText(R.string.unknown)
        }
        itemView.site_container.setOnClickListener {
            site.click()
        }
    }
}
