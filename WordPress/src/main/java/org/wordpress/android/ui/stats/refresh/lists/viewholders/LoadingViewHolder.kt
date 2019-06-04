package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.util.image.ImageManager

class LoadingViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BaseStatsViewHolder(
        parent,
        R.layout.stats_loading_view
) {
    private val list: androidx.recyclerview.widget.RecyclerView = itemView.findViewById(R.id.stats_block_list)
    override fun bind(statsType: StatsStore.StatsType?, items: List<BlockListItem>) {
        super.bind(statsType, items)
        list.isNestedScrollingEnabled = false
        if (list.adapter == null) {
            list.layoutManager = LinearLayoutManager(list.context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
            list.adapter = BlockListAdapter(imageManager)
        }
        (list.adapter as BlockListAdapter).update(items)
    }
}
