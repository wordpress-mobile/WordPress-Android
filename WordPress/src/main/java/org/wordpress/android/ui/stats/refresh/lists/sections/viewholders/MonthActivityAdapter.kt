package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.BoxItem

class MonthActivityAdapter : Adapter<DayViewHolder>() {
    private var items: List<BoxItem> = listOf()
    fun update(newItems: List<BoxItem>) {
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
        private val oldItems: List<BoxItem>,
        private val newItems: List<BoxItem>
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
