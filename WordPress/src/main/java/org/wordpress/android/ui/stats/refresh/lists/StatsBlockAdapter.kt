package org.wordpress.android.ui.stats.refresh.lists

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.EmptyBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Loading
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Success
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.CONTROL
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.values
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BaseStatsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BlockListViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.ControlViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.LoadingViewHolder
import org.wordpress.android.util.image.ImageManager

class StatsBlockAdapter(val imageManager: ImageManager) : Adapter<BaseStatsViewHolder>() {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseStatsViewHolder {
        return when (values()[viewType]) {
            SUCCESS, ERROR, EMPTY -> BlockListViewHolder(parent, imageManager)
            LOADING -> LoadingViewHolder(parent, imageManager)
            CONTROL -> ControlViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        when (holder) {
            is ControlViewHolder -> holder.bind(item.data)
            is BlockListViewHolder,
            is LoadingViewHolder -> {
                when (item) {
                    is Success -> holder.bind(item.statsType, item.data)
                    is Loading -> holder.bind(item.statsType, item.data)
                    is EmptyBlock -> holder.bind(item.statsType, item.data)
                    is Error -> holder.bind(item.statsType, item.data)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
