package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.NOT_IMPLEMENTED

class InsightsAdapter : Adapter<InsightsViewHolder>() {
    private var items: List<InsightsItem> = listOf()
    fun update(newItems: List<InsightsItem>) {
        val diffResult = DiffUtil.calculateDiff(
                InsightsDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightsViewHolder {
        return when (InsightsItem.Type.values()[viewType]) {
            NOT_IMPLEMENTED -> NotImplementedViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: InsightsViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is NotImplementedViewHolder -> holder.bind(item as NotImplemented)
        }
    }
}
