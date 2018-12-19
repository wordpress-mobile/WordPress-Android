package org.wordpress.android.ui.stats.refresh.lists

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.values
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BaseStatsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BlockListViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.ErrorViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.LoadingViewHolder
import org.wordpress.android.util.image.ImageManager

class StatsBlockAdapter(val imageManager: ImageManager) : Adapter<BaseStatsViewHolder<*>>() {
    private var items: List<StatsBlock> = listOf()
    fun update(newItems: List<StatsBlock>) {
        val diffResult = DiffUtil.calculateDiff(
                StatsBlockDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseStatsViewHolder<*> {
        return when (values()[viewType]) {
            BLOCK_LIST -> BlockListViewHolder(parent, imageManager)
            ERROR -> ErrorViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            LOADING -> LoadingViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseStatsViewHolder<*>, position: Int, payloads: List<Any>) {
        val item = items[position]
        when (holder) {
            is BlockListViewHolder -> holder.bind(item as BlockList)
            is LoadingViewHolder -> holder.bind(item as Loading)
            is ErrorViewHolder -> holder.bind(item as Error)
            is EmptyViewHolder -> holder.bind(item as Empty)
            is LoadingViewHolder -> holder.bind(item as Loading)
        }
    }

    override fun onBindViewHolder(holder: BaseStatsViewHolder<*>, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
