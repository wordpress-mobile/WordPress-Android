package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Status.ADDED
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Status.REMOVED
import java.util.Collections

class InsightsManagementAdapter(
    private val onItemButtonClicked: (InsightModel) -> Unit,
    private val onDragStarted: (viewHolder: ViewHolder) -> Unit,
    private val onDragFinished: (List<InsightModel>) -> Unit
) : Adapter<InsightsManagementViewHolder>(), ItemTouchHelperAdapter {
    private var items = ArrayList<InsightModel>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): InsightsManagementViewHolder {
        val type = InsightModel.Status.values()[itemType]
        return when (type) {
            ADDED -> AddedInsightViewHolder(parent, onDragStarted, onItemButtonClicked)
            REMOVED -> RemovedInsightViewHolder(parent, onItemButtonClicked)
        }
    }

    override fun onBindViewHolder(holder: InsightsManagementViewHolder, position: Int) {
        holder.bind(items[position], position == items.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onItemMoved(originalViewHolder: ViewHolder, newViewHolder: ViewHolder) {
        val fromPosition = originalViewHolder.adapterPosition
        val toPosition = newViewHolder.adapterPosition
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)

        (originalViewHolder as? AddedInsightViewHolder)?.updateDividerVisibility(toPosition == items.size - 1)
        (newViewHolder as? AddedInsightViewHolder)?.updateDividerVisibility(fromPosition == items.size - 1)
    }

    override fun onDragFinished(viewHolder: ViewHolder) {
        onDragFinished.invoke(items)

        (viewHolder as? AddedInsightViewHolder)?.onDragFinished()
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<InsightModel>) {
        val diffResult = DiffUtil.calculateDiff(InsightModelDiffCallback(items, newItems))
        items = ArrayList(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
}

interface ItemTouchHelperAdapter {
    fun onItemMoved(originalViewHolder: ViewHolder, newViewHolder: ViewHolder)
    fun onDragFinished(viewHolder: ViewHolder)
}

class InsightModelDiffCallback(
    private val oldList: List<InsightModel>,
    private val newList: List<InsightModel>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].insightType == newList[newItemPosition].insightType
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
