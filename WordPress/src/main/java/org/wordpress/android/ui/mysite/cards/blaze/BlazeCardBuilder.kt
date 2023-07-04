package org.wordpress.android.ui.mysite.cards.blaze

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class BlazeCardBuilder @Inject constructor() {
    fun build(params: MySiteCardAndItemBuilderParams.BlazeCardBuilderParams): BlazeCard? {
        return when (params) {
            is PromoteWithBlazeCardBuilderParams -> buildPromoteWithBlazeCard(params)
            is CampaignWithBlazeCardBuilderParams -> buildBlazeCampaignsCard(params)
        }
    }

    private fun buildBlazeCampaignsCard(params: CampaignWithBlazeCardBuilderParams): BlazeCampaignsCard? {
        TODO("Not yet implemented")
    }

    private fun buildPromoteWithBlazeCard(params: PromoteWithBlazeCardBuilderParams): PromoteWithBlazeCard {
        return PromoteWithBlazeCard(
            title = UiString.UiStringRes(R.string.promote_blaze_card_title),
            subtitle = UiString.UiStringRes(R.string.promote_blaze_card_sub_title),
            onClick = ListItemInteraction.create(params.onClick),
            onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
            onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick)
        )
    }
}
