package org.wordpress.android.ui.stats.refresh

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.R.layout

class ListInsightsViewHolder(parent: ViewGroup) : InsightsViewHolder(parent, layout.stats_list_block) {
    private val list: RecyclerView = itemView.findViewById(R.id.stats_block_list)
    fun bind(item: ListInsightItem) {
        list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
        if (list.adapter == null) {
            list.adapter = BlockListAdapter()
        }
        (list.adapter as BlockListAdapter).update(item.items)
    }
}
