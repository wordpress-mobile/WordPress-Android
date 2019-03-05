package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box

class MonthActivityAdapter : Adapter<DayViewHolder>() {
    private var items: List<Box> = listOf()
    fun update(newItems: List<Box>) {
        val diffResult = DiffUtil.calculateDiff(BoxDiffCallback(items, newItems))
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(parent)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: DayViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        holder.bind(item)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }

    private class BoxDiffCallback(
        private val oldItems: List<Box>,
        private val newItems: List<Box>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldItems[oldPosition] == newItems[newPosition]
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldItems[oldPosition] == newItems[newPosition]
        }
    }
}
