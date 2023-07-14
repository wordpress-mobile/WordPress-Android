package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardFooter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem.BlazeCampaignStats
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

val campaign = BlazeCampaignModel(
    campaignId = 1,
    title = "title",
    imageUrl = "imageUrl",
    startDate = mock(),
    endDate = mock(),
    uiStatus = "uiStatus",
    budgetCents = 20.0,
    impressions = 1,
    clicks = 1,
)

val onCreateCampaignClick = { }
val onCampaignClick = { }
val onCardClick = { }

val campaignWithBlazeBuilderParams = CampaignWithBlazeCardBuilderParams(
    campaign = campaign,
    onCardClick = onCardClick,
    onCampaignClick = onCampaignClick,
    onCreateCampaignClick = onCreateCampaignClick
)

val blazeCampaignsCardModel = BlazeCampaignsCardModel(
    title = UiString.UiStringRes(R.string.blaze_campaigns_card_title),
    campaign = BlazeCampaignsCardItem(
        id = campaign.campaignId,
        title = UiString.UiStringText(campaign.title),
        status = UiString.UiStringText(campaign.uiStatus),
        featuredImageUrl = campaign.imageUrl,
        stats = BlazeCampaignStats(
            impressions = UiString.UiStringText(campaign.impressions.toString()),
            clicks = UiString.UiStringText(campaign.clicks.toString())
        ),
        onClick = onCampaignClick,
    ),
    footer = BlazeCampaignsCardFooter(
        label = UiString.UiStringRes(R.string.blaze_campaigns_card_footer_label),
        onClick = ListItemInteraction.create(onCreateCampaignClick)
    ),
    onClick = ListItemInteraction.create(onCardClick),
)


class BlazeCardBuilderTest {
    private lateinit var builder: BlazeCardBuilder

    @Before
    fun setUp() {
        builder = BlazeCardBuilder()
    }

    @Test
    fun `when promoteBlazeBuilderParams, then return the PromoteWithBlazeCard`() {
        // Arrange
        val params = PromoteWithBlazeCardBuilderParams(
            onClick = { },
            onHideMenuItemClick = { },
            onMoreMenuClick = { }
        )

        // Act
        val result = builder.build(params) as PromoteWithBlazeCard

        // Assert
        assertNotNull(result)
        assertEquals(
            R.string.promote_blaze_card_title,
            (result.title as UiString.UiStringRes).stringRes
        )
        assertEquals(
            R.string.promote_blaze_card_sub_title,
            (result.subtitle as UiString.UiStringRes).stringRes
        )
        assertNotNull(result.onClick)
        assertNotNull(result.onHideMenuItemClick)
        assertNotNull(result.onMoreMenuClick)
    }

    @Test
    fun `when campaignBuilderParams, then return the CampaignCard`() {
        // Arrange
        val params = campaignWithBlazeBuilderParams

        // Act
        val result = builder.build(params) as BlazeCampaignsCardModel

        // Assert
        assertEquals(blazeCampaignsCardModel,result)
    }
}
