package org.wordpress.android.ui.stats.refresh.lists.widget

import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.SiteUiModel
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
