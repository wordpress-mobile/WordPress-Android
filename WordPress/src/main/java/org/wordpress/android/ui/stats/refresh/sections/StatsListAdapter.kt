package org.wordpress.android.ui.stats.refresh.sections

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.sections.viewholders.EmptyViewHolder
import org.wordpress.android.ui.stats.refresh.sections.viewholders.FailedViewHolder
import org.wordpress.android.ui.stats.refresh.StatsDiffCallback
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.LOADING
import org.wordpress.android.ui.stats.refresh.sections.StatsItem.Type.values
import org.wordpress.android.ui.stats.refresh.sections.viewholders.StatsViewHolder
import org.wordpress.android.ui.stats.refresh.sections.viewholders.BlockListViewHolder
import org.wordpress.android.ui.stats.refresh.sections.viewholders.LoadingViewHolder
import org.wordpress.android.util.image.ImageManager

class StatsListAdapter(val imageManager: ImageManager) : Adapter<StatsViewHolder>() {
    private var items: List<StatsItem> = listOf()
    fun update(newItems: List<StatsItem>) {
        val diffResult = DiffUtil.calculateDiff(
                StatsDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        return when (values()[viewType]) {
            BLOCK_LIST -> BlockListViewHolder(
                    parent,
                    imageManager
            )
            FAILED -> FailedViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
            LOADING -> LoadingViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is BlockListViewHolder -> holder.bind(item as ListInsightItem)
            is FailedViewHolder -> holder.bind(item as Failed)
            is EmptyViewHolder -> holder.bind(item as Empty)
        }
    }
}
