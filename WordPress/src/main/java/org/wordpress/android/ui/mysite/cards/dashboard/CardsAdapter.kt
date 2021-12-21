package org.wordpress.android.ui.mysite.cards.dashboard

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.apache.commons.lang3.NotImplementedException
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.cards.dashboard.error.ErrorCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardViewHolder

import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class CardsAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<CardViewHolder<*>>() {
    private val items = mutableListOf<DashboardCard>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder<*> {
        return when (viewType) {
            CardType.POST_CARD_WITHOUT_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithoutPostItemsViewHolder(parent, imageManager, uiHelpers)
            CardType.POST_CARD_WITH_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithPostItemsViewHolder(parent, imageManager, uiHelpers)
            CardType.ERROR_CARD.ordinal -> ErrorCardViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CardViewHolder<*>, position: Int) {
        when (holder) {
            is ErrorCardViewHolder -> holder.bind(items[position])
            is PostCardViewHolder<*> -> holder.bind(items[position])
        }
    }

    override fun getItemViewType(position: Int) = items[position].cardType.ordinal

    fun update(newItems: List<DashboardCard>) {
        val diffResult = DiffUtil.calculateDiff(DashboardCardsDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class DashboardCardsDiffUtil(
        private val oldList: List<DashboardCard>,
        private val newList: List<DashboardCard>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = newList[newItemPosition]
            val oldItem = oldList[oldItemPosition]

            return oldItem.cardType == newItem.cardType && when {
                oldItem is PostCardWithPostItems && newItem is PostCardWithPostItems -> true
                oldItem is PostCardWithoutPostItems && newItem is PostCardWithoutPostItems -> true
                oldItem is ErrorCard && newItem is ErrorCard -> true
                else -> throw NotImplementedException("Diff not implemented yet")
            }
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
