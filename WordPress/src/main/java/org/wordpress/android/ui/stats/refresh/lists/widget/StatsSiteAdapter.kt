package org.wordpress.android.ui.stats.refresh.lists.widget

import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.SiteUiModel
import org.wordpress.android.util.image.ImageManager

class StatsSiteAdapter(
    val imageManager: ImageManager
) : Adapter<StatsSiteViewHolder>() {
    private var sites = mutableListOf<SiteUiModel>()
    override fun onCreateViewHolder(view: ViewGroup, p1: Int): StatsSiteViewHolder {
        return StatsSiteViewHolder(view, imageManager)
    }

    override fun getItemCount(): Int {
        return sites.size
    }

    override fun onBindViewHolder(viewHolder: StatsSiteViewHolder, position: Int) {
        viewHolder.bind(sites[position])
    }

    fun update(updatedSites: List<SiteUiModel>) {
        sites.clear()
        sites.addAll(updatedSites)
        notifyDataSetChanged()
    }
}
