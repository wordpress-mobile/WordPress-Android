package org.wordpress.android.ui.mysite.cards.blaze

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard.BlazeCampaignsCardItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard.BlazeCampaignsCardFooter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class BlazeCardBuilder @Inject constructor() {
    fun build(params: MySiteCardAndItemBuilderParams.BlazeCardBuilderParams): BlazeCard {
        return when (params) {
            is PromoteWithBlazeCardBuilderParams -> buildPromoteWithBlazeCard(params)
            is CampaignWithBlazeCardBuilderParams -> buildBlazeCampaignsCard(params)
        }
    }

    private fun buildBlazeCampaignsCard(params: CampaignWithBlazeCardBuilderParams): BlazeCampaignsCard {
        return BlazeCampaignsCard(
            title = UiString.UiStringRes(R.string.blaze_campaigns_card_title),
            campaign = getRecentCampaign(params),
            footer = getBlazeCardFooter(params),
            onClick = ListItemInteraction.create(params.onCardClick),
        )
    }

    private fun getBlazeCardFooter(params: CampaignWithBlazeCardBuilderParams): BlazeCampaignsCardFooter {
        return BlazeCampaignsCardFooter(
            label = UiString.UiStringRes(R.string.blaze_campaigns_card_footer_label),
            onClick = ListItemInteraction.create(params.onCreateCampaignClick),
        )
    }

    private fun getRecentCampaign(params: CampaignWithBlazeCardBuilderParams): BlazeCampaignsCardItem {
        with(params) {
            return BlazeCampaignsCardItem(
                id = campaign.campaignId,
                title = UiString.UiStringText(campaign.title),
                status = UiString.UiStringText(campaign.uiStatus),
                featuredImageUrl = campaign.imageUrl,
                stats = BlazeCampaignsCardItem.BlazeCampaignStats(
                    impressionCount = UiString.UiStringText(campaign.impressions.toString()),
                    clickCount = UiString.UiStringText(campaign.clicks.toString()),
                ),
                onClick = params.onCampaignClick,
            )
        }
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
