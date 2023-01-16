package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.utils.WrappingLinearLayoutManager
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
            val blockListAdapter = BlockListAdapter(imageManager)
            val layoutManager = WrappingLinearLayoutManager(
                list.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            list.adapter = blockListAdapter
            list.layoutManager = layoutManager
        }
        (list.layoutManager as WrappingLinearLayoutManager).init()
        if (list.adapter?.itemCount ?: 0 > items.size) {
            (list.layoutManager as? WrappingLinearLayoutManager)?.onItemRangeRemoved()
        }
        (list.adapter as BlockListAdapter).update(items)
    }
}
