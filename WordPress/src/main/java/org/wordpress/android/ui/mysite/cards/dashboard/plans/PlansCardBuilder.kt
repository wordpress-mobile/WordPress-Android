package org.wordpress.android.ui.mysite.cards.dashboard.plans

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardPlansBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class PlansCardBuilder @Inject constructor() {
    fun build(params: DashboardCardPlansBuilderParams): DashboardPlansCard? {
        return if (params.isEligible) {
            DashboardPlansCard(
                title = UiString.UiStringRes(R.string.dashboard_card_plans_title),
                subtitle = UiString.UiStringRes(R.string.dashboard_card_plans_sub_title),
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
