package org.wordpress.android.ui.jetpack.scan.details.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState.ThreatContextLineItemState
import org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders.ThreatContextLineViewHolder

class ThreatContextLinesAdapter : Adapter<ThreatContextLineViewHolder>() {
    private val items = mutableListOf<ThreatContextLineItemState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ThreatContextLineViewHolder(parent)

    override fun onBindViewHolder(holder: ThreatContextLineViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<ThreatContextLineItemState>) {
        val diffResult = DiffUtil.calculateDiff(ThreatContextLineDiffCallback(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)

        diffResult.dispatchUpdatesTo(this)
    }

    class ThreatContextLineDiffCallback(
        private val oldList: List<ThreatContextLineItemState>,
        private val newList: List<ThreatContextLineItemState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
