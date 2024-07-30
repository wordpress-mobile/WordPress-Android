package org.wordpress.android.ui.mysite.cards.dashboard.activity

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.utils.UiHelpers
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ActivityCard.ActivityCardWithItems.ActivityItem

class ActivityItemsAdapter(
    private val uiHelpers: UiHelpers
) : Adapter<ActivityItemViewHolder>() {
    private val items = mutableListOf<ActivityItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ActivityItemViewHolder(
        parent,
        uiHelpers
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ActivityItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun update(newItems: List<ActivityItem>) {
        val diffResult = DiffUtil.calculateDiff(ActivityItemDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }

    class ActivityItemDiffUtil(
        private val oldList: List<ActivityItem>,
        private val newList: List<ActivityItem>
    ) : DiffUtil.Callback() {
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

