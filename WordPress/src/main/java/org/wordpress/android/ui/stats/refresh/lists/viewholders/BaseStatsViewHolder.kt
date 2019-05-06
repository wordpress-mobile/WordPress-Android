package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.support.annotation.CallSuper
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.OVERVIEW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

abstract class BaseStatsViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    @CallSuper
    open fun bind(statsType: StatsType?, items: List<BlockListItem>) {
        if (statsType == OVERVIEW || statsType == LATEST_POST_SUMMARY) {
            val layoutParams = itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
            layoutParams?.isFullSpan = true
        }
    }
}
