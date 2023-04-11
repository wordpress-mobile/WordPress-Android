package org.wordpress.android.ui.mysite.cards.dashboard.domain

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardDomainCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardDomainBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class DashboardDomainCardBuilder @Inject constructor() {
    fun build(params: DashboardCardDomainBuilderParams):DashboardDomainCard? {
        return if (params.isEligible) {
            DashboardDomainCard(
                title = UiString.UiStringRes(R.string.dashboard_card_domain_title),
                subtitle = UiString.UiStringRes(R.string.dashboard_card_domain_sub_title),
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
