package org.wordpress.android.ui.mysite.cards.dashboard

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.MySiteDashboardCardsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.viewBinding

class DashboardCardsViewHolder(
    parentView: ViewGroup,
    imageManager: ImageManager,
    uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteDashboardCardsBinding>(
        parentView.viewBinding(MySiteDashboardCardsBinding::inflate)
) {
    init {
        with(binding.dashboardCards) {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = DashboardCardsAdapter(imageManager, uiHelpers)
        }
    }

    fun bind(cards: DashboardCards) = with(binding) {
        (dashboardCards.adapter as DashboardCardsAdapter).update(cards.cards)
    }
}
