package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.support.annotation.CallSuper
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.DATE
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock

abstract class BaseStatsViewHolder<T : StatsBlock>(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    @CallSuper
    open fun bind(item: T) {
        if (item.statsTypes == OVERVIEW || item.statsTypes == LATEST_POST_SUMMARY || item.statsTypes == DATE) {
            val layoutParams = itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
            layoutParams?.isFullSpan = true
        }
    }
}
