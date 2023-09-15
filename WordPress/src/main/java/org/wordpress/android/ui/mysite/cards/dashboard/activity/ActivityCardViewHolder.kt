package org.wordpress.android.ui.mysite.cards.dashboard.activity

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteActivityCardWithActivityItemsBinding
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction

class ActivityCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteActivityCardWithActivityItemsBinding>(
    parent.viewBinding(MySiteActivityCardWithActivityItemsBinding::inflate)
) {
    init {
        binding.activityItems.adapter = ActivityItemsAdapter(uiHelpers)
    }

    fun bind(card: ActivityCard) = with(binding) {
        val activityCard = card as ActivityCard.ActivityCardWithItems
        (activityItems.adapter as ActivityItemsAdapter).update(activityCard.activityItems)
        mySiteToolbar.update(activityCard)
    }

    private fun MySiteCardToolbarBinding.update(card: ActivityCard.ActivityCardWithItems) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, card.title)
        mySiteCardToolbarMore.visibility = View.VISIBLE
        mySiteCardToolbarMore.setOnClickListener {
            showMoreMenu(
                card.onHideMenuItemClick,
                card.onAllActivityMenuItemClick,
                card.onMoreMenuClick,
                mySiteCardToolbarMore,
            )
        }
    }

    private fun showMoreMenu(
        onHideMenuItemClick: ListItemInteraction,
        onAllActivityItemClick: ListItemInteraction,
        onMoreMenuClick: ListItemInteraction,
        anchor: View) {
        onMoreMenuClick.click()
        val popupMenu = PopupMenu(itemView.context, anchor)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.activity_card_menu_item_all_activity -> {
                    onAllActivityItemClick.click()
                    return@setOnMenuItemClickListener true
                }

                R.id.activity_card_menu_item_hide_this -> {
                    onHideMenuItemClick.click()
                    return@setOnMenuItemClickListener true
                }

                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.activity_log_card_menu)
        popupMenu.show()
    }
}
