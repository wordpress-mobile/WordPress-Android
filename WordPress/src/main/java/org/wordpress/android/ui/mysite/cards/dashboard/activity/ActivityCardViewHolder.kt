package org.wordpress.android.ui.mysite.cards.dashboard.activity

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteActivityCardWithActivityItemsBinding
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.utils.UiString

class ActivityCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteActivityCardWithActivityItemsBinding>(
    parent.viewBinding(MySiteActivityCardWithActivityItemsBinding::inflate)
) {
    init {
        binding.activityItems.adapter = ActivityItemsAdapter(uiHelpers)
    }

    fun bind(card: ActivityCard) = with(binding) {
        val activityCard = card as ActivityCard.ActivityCardWithItems
        (activityItems.adapter as ActivityItemsAdapter).update(activityCard.activityItems)
        mySiteToolbar.update(activityCard.title)
    }

    private fun MySiteCardToolbarBinding.update(title: UiString?) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
    }
}
