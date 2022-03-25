package org.wordpress.android.ui.mysite.cards.dashboard.error

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySiteErrorWithinCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorWithinCard
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.viewBinding

class ErrorWithinCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteErrorWithinCardBinding>(
        parent.viewBinding(MySiteErrorWithinCardBinding::inflate)
) {
    fun bind(card: ErrorWithinCard) = with(binding) {
        mySiteToolbar.update(card.title)
    }

    private fun MySiteCardToolbarBinding.update(title: UiString) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
    }
}
