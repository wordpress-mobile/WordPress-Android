package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.DATE
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.util.image.ImageManager

class BlockListViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BaseStatsViewHolder<BlockList>(
        parent,
        layout.stats_list_block
) {
    private val container: CardView = itemView.findViewById(R.id.container)
    private val list: RecyclerView = itemView.findViewById(R.id.stats_block_list)
    override fun bind(item: BlockList) {
        super.bind(item)
        if (item.statsTypes == DATE) {
            container.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.transparent))
            container.cardElevation = 0F
        } else {
            container.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
            container.cardElevation = itemView.resources.getDimension(R.dimen.cardview_default_elevation)
        }
        list.isNestedScrollingEnabled = false
        if (list.adapter == null) {
            list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
            list.adapter = BlockListAdapter(imageManager)
        }
        (list.adapter as BlockListAdapter).update(item.items)
    }
}
