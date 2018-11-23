package org.wordpress.android.ui.stats.refresh

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.util.image.ImageManager

class ListInsightsViewHolder(parent: ViewGroup, val imageManager: ImageManager) : InsightsViewHolder(
        parent,
        layout.stats_list_block
) {
    private val list: RecyclerView = itemView.findViewById(R.id.stats_block_list)
    fun bind(item: ListInsightItem) {
        list.isNestedScrollingEnabled = false
        Log.d("list_insights", "Previous adapter data count: ${list.adapter?.itemCount}")
        Log.d("list_insights", "Current data count: ${item.items.size} - state: ${item.uiState}")
        if (list.adapter == null) {
            list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
            list.adapter = BlockListAdapter(imageManager)
        }
        (list.adapter as BlockListAdapter).update(item.items)
    }
}
