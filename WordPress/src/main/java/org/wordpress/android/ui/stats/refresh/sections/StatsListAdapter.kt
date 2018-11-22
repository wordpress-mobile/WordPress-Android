package org.wordpress.android.ui.stats.refresh.sections

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.Empty
import org.wordpress.android.ui.stats.refresh.EmptyInsightsViewHolder
import org.wordpress.android.ui.stats.refresh.Failed
import org.wordpress.android.ui.stats.refresh.FailedViewHolder
import org.wordpress.android.ui.stats.refresh.InsightsDiffCallback
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LOADING
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.values
import org.wordpress.android.ui.stats.refresh.InsightsViewHolder
import org.wordpress.android.ui.stats.refresh.ListInsightItem
import org.wordpress.android.ui.stats.refresh.ListInsightsViewHolder
import org.wordpress.android.ui.stats.refresh.LoadingInsightsViewHolder
import org.wordpress.android.util.image.ImageManager

class StatsListAdapter(val imageManager: ImageManager) : Adapter<InsightsViewHolder>() {
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
            LIST_INSIGHTS -> ListInsightsViewHolder(parent, imageManager)
            FAILED -> FailedViewHolder(parent)
            EMPTY -> EmptyInsightsViewHolder(parent)
            LOADING -> LoadingInsightsViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: InsightsViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ListInsightsViewHolder -> holder.bind(item as ListInsightItem)
            is FailedViewHolder -> holder.bind(item as Failed)
            is EmptyInsightsViewHolder -> holder.bind(item as Empty)
        }
    }
}
