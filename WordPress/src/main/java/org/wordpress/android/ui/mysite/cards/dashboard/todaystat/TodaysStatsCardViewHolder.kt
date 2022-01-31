package org.wordpress.android.ui.mysite.cards.dashboard.todaystat

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteTodaysStatsCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class TodaysStatsCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteTodaysStatsCardBinding>(
        parent.viewBinding(MySiteTodaysStatsCardBinding::inflate)
) {
    fun bind(card: TodaysStatsCard) = with(binding) {
        uiHelpers.setTextOrHide(viewsCount, card.views)
        uiHelpers.setTextOrHide(visitorsCount, card.visitors)
        uiHelpers.setTextOrHide(likesCount, card.likes)
    }
}
