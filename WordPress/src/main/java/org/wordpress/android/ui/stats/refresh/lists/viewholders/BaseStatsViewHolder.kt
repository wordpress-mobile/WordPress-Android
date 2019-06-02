package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.support.annotation.CallSuper
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.StaggeredGridLayoutManager.LayoutParams
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.OVERVIEW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

abstract class BaseStatsViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    @CallSuper
    open fun bind(statsType: StatsType?, items: List<BlockListItem>) {
        when (statsType) {
            OVERVIEW, LATEST_POST_SUMMARY -> {
                setFullWidth()
            }
            ManagementType.CONTROL -> {
                itemView.background = null
                setFullWidth()
            }
        }
    }

    private fun setFullWidth() {
        val layoutParams = itemView.layoutParams as? LayoutParams
        layoutParams?.isFullSpan = true
    }
}
