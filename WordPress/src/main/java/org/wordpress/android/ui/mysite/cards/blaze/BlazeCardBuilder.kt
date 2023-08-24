package org.wordpress.android.ui.mysite.cards.blaze

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardFooter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

class BlazeCardBuilder @Inject constructor(private val statsUtils: StatsUtils) {
    fun build(params: MySiteCardAndItemBuilderParams.BlazeCardBuilderParams): BlazeCard {
        return when (params) {
            is PromoteWithBlazeCardBuilderParams -> buildPromoteWithBlazeCard(params)
            is CampaignWithBlazeCardBuilderParams -> buildBlazeCampaignsCard(params)
        }
    }

    private fun buildBlazeCampaignsCard(params: CampaignWithBlazeCardBuilderParams): BlazeCampaignsCardModel {
        return BlazeCampaignsCardModel(
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
                status = CampaignStatus.fromString(campaign.uiStatus),
                featuredImageUrl = campaign.imageUrl,
                stats = if (shouldShowCampaignStatus(campaign.uiStatus)) getCampaignStats(campaign) else null,
                onClick = params.onCampaignClick,
            )
        }
    }

    private fun shouldShowCampaignStatus(campaignStatus: String): Boolean {
        return campaignStatus == CampaignStatus.Active.status || campaignStatus == CampaignStatus.Completed.status
    }

    private fun getCampaignStats(campaign: BlazeCampaignModel): BlazeCampaignsCardItem.BlazeCampaignStats {
        return BlazeCampaignsCardItem.BlazeCampaignStats(
            impressions = mapToStatsString(campaign.impressions),
            clicks = mapToStatsString(campaign.clicks),
        )
    }

    private fun mapToStatsString(value: Long): UiString {
        val formattedString = statsUtils.toFormattedString(value, ONE_THOUSAND)
        return UiString.UiStringText(formattedString)
    }

    private fun buildPromoteWithBlazeCard(params: PromoteWithBlazeCardBuilderParams): PromoteWithBlazeCard {
        return PromoteWithBlazeCard(
            title = UiString.UiStringRes(R.string.promote_blaze_card_title),
            subtitle = UiString.UiStringRes(R.string.promote_blaze_card_sub_title),
            onClick = ListItemInteraction.create(params.onClick),
            moreMenuOptions = PromoteWithBlazeCard.MoreMenuOptions(
                onMoreClick = ListItemInteraction.create(params.moreMenuParams.onMoreMenuClick),
                hideThisMenuItemClick = ListItemInteraction.create(params.moreMenuParams.onHideThisCardItemClick),
                learnMoreClick = ListItemInteraction.create(params.moreMenuParams.onLearnMoreClick)
            )
        )
    }
}
