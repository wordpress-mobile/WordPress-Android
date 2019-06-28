package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel.SiteUiModel
import org.wordpress.android.util.image.ImageManager

class StatsWidgetSiteAdapter(
    val imageManager: ImageManager
) : Adapter<StatsWidgetSiteViewHolder>() {
    private var sites = mutableListOf<SiteUiModel>()
    override fun onCreateViewHolder(view: ViewGroup, p1: Int): StatsWidgetSiteViewHolder {
        return StatsWidgetSiteViewHolder(view, imageManager)
    }

    override fun getItemCount(): Int {
        return sites.size
    }

    override fun onBindViewHolder(viewHolder: StatsWidgetSiteViewHolder, position: Int) {
        viewHolder.bind(sites[position])
    }

    fun update(updatedSites: List<SiteUiModel>) {
        sites.clear()
        sites.addAll(updatedSites)
        notifyDataSetChanged()
    }
}
