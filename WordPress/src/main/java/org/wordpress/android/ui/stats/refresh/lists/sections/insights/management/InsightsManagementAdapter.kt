package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightModelDiffCallback.Payload
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewHolder.HeaderViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewHolder.InsightViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status.ADDED
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Type
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Type.INSIGHT

class InsightsManagementAdapter : Adapter<InsightsManagementViewHolder>() {
    private var items = ArrayList<InsightListItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        itemType: Int
    ): InsightsManagementViewHolder {
        return when (Type.values()[itemType]) {
            HEADER -> HeaderViewHolder(parent)
            INSIGHT -> InsightViewHolder(parent)
        }
    }

    override fun onBindViewHolder(
        holder: InsightsManagementViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(
                    item as Header,
                    position == 0
            )
            is InsightViewHolder -> holder.bind(
                    item as InsightModel,
                    payloads.firstOrNull() as? Payload
            )
        }
    }

    override fun onBindViewHolder(holder: InsightsManagementViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf<Payload>())
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<InsightListItem>) {
        val diffResult = DiffUtil.calculateDiff(InsightModelDiffCallback(items, newItems))
        items = ArrayList(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
}

class InsightModelDiffCallback(
    private val oldList: List<InsightListItem>,
    private val newList: List<InsightListItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is Header && newItem is Header) {
            oldItem.text == newItem.text
        } else if (oldItem is InsightModel && newItem is InsightModel) {
            oldItem.insightType == newItem.insightType
        } else {
            false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (oldItem is InsightModel && newItem is InsightModel && oldItem.status != newItem.status) {
            return Payload(newItem.status == ADDED)
        }
        return null
    }

    data class Payload(val isSelected: Boolean)
}
