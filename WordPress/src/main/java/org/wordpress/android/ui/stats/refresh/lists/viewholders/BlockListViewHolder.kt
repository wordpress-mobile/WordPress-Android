package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.util.image.ImageManager

open class BlockListViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BaseStatsViewHolder(
        parent,
        R.layout.stats_list_block
) {
    private val list: RecyclerView = itemView.findViewById(R.id.stats_block_list)
    override fun bind(statsType: StatsType?, items: List<BlockListItem>) {
        super.bind(statsType, items)
        list.isNestedScrollingEnabled = false
        if (list.adapter == null) {
            list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
            list.adapter = BlockListAdapter(imageManager)
        }
        (list.adapter as BlockListAdapter).update(items)
    }
}
