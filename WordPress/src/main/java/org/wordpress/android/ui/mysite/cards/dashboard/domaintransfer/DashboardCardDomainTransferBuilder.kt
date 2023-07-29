package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardDomainTransferBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class DashboardCardDomainTransferBuilder @Inject constructor() {
    fun build(params: DashboardCardDomainTransferBuilderParams): DomainTransferCardModel? {
        return if (params.isEligible) {
            DomainTransferCardModel(
                title = UiString.UiStringRes(R.string.domain_transfer_card_title),
                subtitle = UiString.UiStringRes(R.string.domain_transfer_card_sub_title),
                caption = UiString.UiStringRes(R.string.domain_transfer_card_caption),
                cta = UiString.UiStringRes(R.string.learn_more),
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
