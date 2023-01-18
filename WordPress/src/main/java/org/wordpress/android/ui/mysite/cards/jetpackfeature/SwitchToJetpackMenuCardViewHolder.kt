package org.wordpress.android.ui.mysite.cards.jetpackfeature

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.SwitchToJetpackMenuBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.extensions.viewBinding

class SwitchToJetpackMenuCardViewHolder(
    parent: ViewGroup,
) : MySiteCardAndItemViewHolder<SwitchToJetpackMenuBinding>(
    parent.viewBinding(SwitchToJetpackMenuBinding::inflate)
) {
    fun bind(card: MySiteCardAndItem.Card.JetpackSwitchMenu) = with(binding) {
        mySiteSwitchToJetpackCard.setOnClickListener { card.onClick.click() }
        switchToAppMoreIcon.setOnClickListener {
            showMoreMenu(
                card.onHideMenuItemClick,
                card.onRemindMeLaterItemClick,
                card.onMoreMenuClick,
                switchToAppMoreIcon,
            )
        }
    }

    private fun showMoreMenu(
        onHideMenuItemClick: ListItemInteraction,
        onRemindMeLaterClick: ListItemInteraction,
        onMoreMenuClick: ListItemInteraction,
        anchor: View
    ) {
        onMoreMenuClick.click()
        val popupMenu = PopupMenu(itemView.context, anchor)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.jetpack_card_menu_item_remind_me_later -> {
                    onRemindMeLaterClick.click()
                    return@setOnMenuItemClickListener true
                }
                R.id.jetpack_card_menu_item_hide_this -> {
                    onHideMenuItemClick.click()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.jetpack_feature_card_menu)
        popupMenu.show()
    }
}
