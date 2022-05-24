package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem

class QuickLinkRibbonItemAdapter : Adapter<QuickLinkRibbonItemViewHolder>() {
    private val items = mutableListOf<QuickLinkRibbonItem>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickLinkRibbonItemViewHolder {
        return QuickLinkRibbonItemViewHolder(parent)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: QuickLinkRibbonItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<QuickLinkRibbonItem>) {
        val diffResult = DiffUtil.calculateDiff(InterestDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class InterestDiffUtil(
        private val oldList: List<QuickLinkRibbonItem>,
        private val newList: List<QuickLinkRibbonItem>
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
