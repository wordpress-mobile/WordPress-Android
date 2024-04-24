package org.wordpress.android.ui.stats.refresh.lists

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.values
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BaseStatsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BlockListViewHolder
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
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    fun positionOf(statsType: StatsType): Int {
        return items.indexOfFirst { it.statsType == statsType }
    }

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        holder.bind(item.statsType, item.data)
    }

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
