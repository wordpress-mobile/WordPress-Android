package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.NOT_IMPLEMENTED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.values

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
        return when (values()[viewType]) {
            NOT_IMPLEMENTED -> NotImplementedViewHolder(parent)
            LIST_INSIGHTS -> ListInsightsViewHolder(parent)
            FAILED -> FailedViewHolder(parent)
            EMPTY -> EmptyViewHolder(parent)
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
            is ListInsightsViewHolder -> holder.bind(item as ListInsightItem)
            is FailedViewHolder -> holder.bind(item as Failed)
            is EmptyViewHolder -> holder.bind(item as Empty)
        }
    }
}
