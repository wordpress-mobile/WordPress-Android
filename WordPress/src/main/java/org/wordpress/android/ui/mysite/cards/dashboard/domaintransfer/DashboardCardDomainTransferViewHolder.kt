package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.DashboardCardDomainTransferBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class DashboardCardDomainTransferViewHolder (
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<DashboardCardDomainTransferBinding>(
    parent.viewBinding(DashboardCardDomainTransferBinding::inflate)
) {
    fun bind(card: DomainTransferCardModel) = with(binding) {
        uiHelpers.setTextOrHide(dashboardCardDomainTransferTitle, card.title)
        uiHelpers.setTextOrHide(dashboardCardDomainTransferSubTitle, card.subtitle)
        dashboardCardDomainTransferCta.setOnClickListener { card.onClick.click() }
        dashboardDomainTransferCardMore.setOnClickListener {
            showMoreMenu(
                card.onHideMenuItemClick,
                card.onMoreMenuClick,
                dashboardDomainTransferCardMore,
            )
        }
        dashboardCardDomainTransferLearnMore.setOnClickListener { card.onClick.click() }
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
                R.id.dashboard_card_domain_transfer_menu_item_hide_this -> {
                    onHideMenuItemClick.click()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.dashboard_card_domain_transfer_menu)
        popupMenu.show()
    }
}
