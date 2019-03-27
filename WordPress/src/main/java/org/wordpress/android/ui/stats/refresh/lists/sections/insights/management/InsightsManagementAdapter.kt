package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.support.v7.util.DiffUtil
import android.support.v7.util.DiffUtil.Callback
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel
import java.util.Collections

class InsightsManagementAdapter(
    private val onStartDrag: ((viewHolder: ViewHolder) -> Unit)? = null,
    private val ondDragFinished: ((List<InsightModel>) -> Unit)? = null
) : Adapter<InsightsManagementViewHolder>(), ItemTouchHelperAdapter {
    private var items = ArrayList<InsightModel>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): InsightsManagementViewHolder {
        return InsightsManagementViewHolder(parent, onStartDrag)
    }

    override fun onBindViewHolder(holder: InsightsManagementViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
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
    }

    override fun onDragFinished() {
        ondDragFinished?.invoke(items)
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<InsightModel>) {
        if (newItems.size >= items.size) {
            val diffResult = DiffUtil.calculateDiff(InsightModelDiffCallback(items, newItems))
            items = ArrayList(newItems)
            diffResult.dispatchUpdatesTo(this)
        } else {
            items = ArrayList(newItems)
            notifyDataSetChanged()
        }
    }
}

interface ItemTouchHelperAdapter {
    fun onItemMoved(fromPosition: Int, toPosition: Int)
    fun onDragFinished()
}

class InsightModelDiffCallback(
    private val oldList: List<InsightModel>,
    private val newList: List<InsightModel>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].insightsTypes == newList[newItemPosition].insightsTypes
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
