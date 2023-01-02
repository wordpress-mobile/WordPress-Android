package org.wordpress.android.ui.mysite.cards.jetpackfeature

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.JetpackFeatureCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class JetpackFeatureCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<JetpackFeatureCardBinding>(
        parent.viewBinding(JetpackFeatureCardBinding::inflate)
) {
    fun bind(card: JetpackFeatureCard) = with(binding) {
        mySiteJetpackFeatureCardCta.setOnClickListener { card.onClick.click() }
        mySiteJetpackFeatureCardLearnMore.setOnClickListener { card.onLearnMoreClick.click() }
        mySiteJetpackFeatureCardMore.setOnClickListener {
            showMoreMenu(
                    card.onHideMenuItemClick,
                    card.onRemindMeLaterItemClick,
                    card.onMoreMenuClick,
                    mySiteJetpackFeatureCardMore,
            )
        }
        uiHelpers.updateVisibility(mySiteJetpackFeatureCardLearnMore, !card.learnMoreUrl.isNullOrEmpty())
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
