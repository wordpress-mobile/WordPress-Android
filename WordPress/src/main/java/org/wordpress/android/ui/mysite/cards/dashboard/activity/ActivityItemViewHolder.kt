package org.wordpress.android.ui.mysite.cards.dashboard.activity

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteActivityCardItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard.ActivityCardWithItems.ActivityItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class ActivityItemViewHolder(parent: ViewGroup,
                             private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteActivityCardItemBinding>(
    parent.viewBinding(MySiteActivityCardItemBinding::inflate)
) {
    fun bind(item: ActivityItem) = with(binding) {
        activityContentContainer.setOnClickListener { item.onClick.click() }
        uiHelpers.setTextOrHide(activityCardItemLabel, item.label)
        uiHelpers.setTextOrHide(activityCardItemSubLabel, item.subLabel)
        uiHelpers.setTextOrHide(activityCardItemDisplayDate, item.displayDate)
        icon.setImageResource(item.icon)
        icon.setBackgroundResource(item.iconBackgroundColor)
    }
}
