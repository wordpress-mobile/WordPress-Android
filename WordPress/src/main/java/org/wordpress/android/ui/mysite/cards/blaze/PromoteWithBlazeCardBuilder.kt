package org.wordpress.android.ui.mysite.cards.blaze

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

import javax.inject.Inject

class PromoteWithBlazeCardBuilder @Inject constructor() {
    fun build(params: MySiteCardAndItemBuilderParams.PromoteWithBlazeCardBuilderParams): PromoteWithBlazeCard? {
        return if (params.isEligible) {
            PromoteWithBlazeCard(
                title = UiString.UiStringRes(R.string.promote_blaze_card_title),
                subtitle = UiString.UiStringRes(R.string.promote_blaze_card_sub_title),
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
