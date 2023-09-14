package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainTransferCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class DomainTransferCardBuilder @Inject constructor() {
    fun build(params: DomainTransferCardBuilderParams?): DomainTransferCardModel? {
        return if (params?.isEligible == true) {
            DomainTransferCardModel(
                title = R.string.domain_transfer_card_title,
                subtitle = R.string.domain_transfer_card_sub_title,
                caption = R.string.domain_transfer_card_caption,
                cta = R.string.domain_transfer_card_cta,
                onClick = ListItemInteraction.create(params.onClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
            )
        } else {
            null
        }
    }
}
