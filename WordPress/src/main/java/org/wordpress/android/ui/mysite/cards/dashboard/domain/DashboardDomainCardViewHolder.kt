package org.wordpress.android.ui.mysite.cards.dashboard.domain

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.DashboardCardDomainBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardDomainCard
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class DashboardDomainCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<DashboardCardDomainBinding>(
    parent.viewBinding(DashboardCardDomainBinding::inflate)
) {
    fun bind(card: DashboardDomainCard) = with(binding) {
        uiHelpers.setTextOrHide(dashboardCardDomainTitle, card.title)
        dashboardCardDomainCta.setOnClickListener { card.onClick.click() }
        dashboardDomainCardMore.setOnClickListener {
            showMoreMenu(
                card.onHideMenuItemClick,
                card.onMoreMenuClick,
                dashboardDomainCardMore,
            )
        }
    }

    private fun showMoreMenu(
        onHideMenuItemClick: ListItemInteraction,
        onMoreMenuClick: ListItemInteraction,
        anchor: View
    ) {
        onMoreMenuClick.click()
        val popupMenu = PopupMenu(itemView.context, anchor)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.dashboard_card_domain_menu_item_hide_this -> {
                    onHideMenuItemClick.click()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.dashboard_card_domain_menu)
        popupMenu.show()
    }
}
