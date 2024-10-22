package org.wordpress.android.ui.mysite.cards.dynamiccard

import android.view.ViewGroup
import org.wordpress.android.databinding.DynamicDashboardCardBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2WithoutBackground
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class DynamicDashboardCardViewHolder(parent: ViewGroup) :
    MySiteCardAndItemViewHolder<DynamicDashboardCardBinding>(parent.viewBinding(DynamicDashboardCardBinding::inflate)) {
    fun bind(card: MySiteCardAndItem.Card.Dynamic) = with(binding) {
        dynamicCard.setContent {
            AppThemeM2WithoutBackground {
                DynamicDashboardCard(card)
            }
        }
    }
}
