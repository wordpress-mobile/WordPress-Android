package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem

class PagesItemsAdapter(
    private val uiHelpers: UiHelpers
) : Adapter<PagesItemViewHolder>() {
    private val items = mutableListOf<PageContentItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PagesItemViewHolder(
        parent,
        uiHelpers
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PagesItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun update(newItems: List<PageContentItem>) {
        val diffResult = DiffUtil.calculateDiff(PagesItemDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class PagesItemDiffUtil(
        private val oldList: List<PageContentItem>,
        private val newList: List<PageContentItem>
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
        ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
