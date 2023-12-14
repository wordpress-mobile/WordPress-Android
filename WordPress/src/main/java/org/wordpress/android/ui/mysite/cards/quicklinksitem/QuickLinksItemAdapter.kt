package org.wordpress.android.ui.mysite.cards.quicklinksitem

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem

class QuickLinksItemAdapter : Adapter<QuickLinksItemViewHolder>() {
    private val items = mutableListOf<QuickLinkItem>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickLinksItemViewHolder {
        return QuickLinksItemViewHolder(parent)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: QuickLinksItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<QuickLinkItem>) {
        val diffResult = DiffUtil.calculateDiff(InterestDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class InterestDiffUtil(
        private val oldList: List<QuickLinkItem>,
        private val newList: List<QuickLinkItem>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = newList[newItemPosition]
            val oldItem = oldList[oldItemPosition]

            return (oldItem == newItem)
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return if (oldList[oldItemPosition].showFocusPoint || newList[newItemPosition].showFocusPoint) {
                false
            } else {
                oldList[oldItemPosition] == newList[newItemPosition]
            }
        }
    }
}
