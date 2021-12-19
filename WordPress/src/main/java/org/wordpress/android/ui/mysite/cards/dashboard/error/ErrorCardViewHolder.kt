package org.wordpress.android.ui.mysite.cards.dashboard.error

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteErrorCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.cards.dashboard.DashboardCardViewHolder
import org.wordpress.android.util.viewBinding

class ErrorCardViewHolder(
    parent: ViewGroup
) : DashboardCardViewHolder<MySiteErrorCardBinding>(
        parent.viewBinding(MySiteErrorCardBinding::inflate)
) {
    fun bind(card: DashboardCard) = with(binding) {
    }
}
