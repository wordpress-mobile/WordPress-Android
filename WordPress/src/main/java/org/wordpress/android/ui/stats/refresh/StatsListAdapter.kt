package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.StatsType.NOT_IMPLEMENTED

class StatsListAdapter : Adapter<StatsViewHolder>() {
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
        return when (StatsType.values()[viewType]) {
            NOT_IMPLEMENTED -> NotImplementedViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is NotImplementedViewHolder -> holder.bind(item as NotImplemented)
        }
    }
}
