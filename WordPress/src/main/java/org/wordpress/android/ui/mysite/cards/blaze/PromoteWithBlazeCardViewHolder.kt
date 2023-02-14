package org.wordpress.android.ui.mysite.cards.blaze

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.PromoteWithBlazeCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class PromoteWithBlazeCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<PromoteWithBlazeCardBinding>(
    parent.viewBinding(PromoteWithBlazeCardBinding::inflate)
) {
    fun bind(card: MySiteCardAndItem.Card.PromoteWithBlazeCard) = with(binding) {
        uiHelpers.setTextOrHide(mySitePromoteWithBlazeCardTitle, card.title)
        mySitePromoteWithBlazeCardCta.setOnClickListener { card.onClick.click() }
        mySitePromoteWithBlazeCardMore.setOnClickListener {
            showMoreMenu(
                card.onHideMenuItemClick,
                card.onMoreMenuClick,
                mySitePromoteWithBlazeCardMore,
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
                R.id.promote_with_blaze_card_menu_item_hide_this -> {
                    onHideMenuItemClick.click()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.promote_with_blaze_card_menu)
        popupMenu.show()
    }
}
