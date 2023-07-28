package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardDomainTransferCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardDomainTransferBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class DashboardCardDomainTransferBuilder @Inject constructor() {
    fun build(params: DashboardCardDomainTransferBuilderParams): DashboardDomainTransferCard? {
        return if (params.isEligible) {
            DashboardDomainTransferCard(
                title = UiString.UiStringRes(R.string.dashboard_card_domain_transfer_title),
                subtitle = UiString.UiStringRes(R.string.dashboard_card_domain_transfer_sub_title),
                caption = UiString.UiStringRes(R.string.dashboard_card_domain_transfer_caption),
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
