package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardFooter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem.BlazeCampaignStats
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

val campaign = BlazeCampaignModel(
    campaignId = "1234",
    title = "title",
    imageUrl = "imageUrl",
    startTime = mock(),
    durationInDays = 1,
    uiStatus = "active",
    impressions = 1,
    clicks = 1,
    targetUrn = null,
    totalBudget = 0.0,
    spentBudget = 0.0,
)

val onCreateCampaignClick = { }
val onCardClick = { }

val onHideCardMenuItemClick = { }
val onMoreMenuClick = { }
val onLearnMoreItemClick = { }
val viewAllCampaignsClick = { }

private var onCampaignClick: ((campaignId: String) -> Unit) = { }
val campaignWithBlazeBuilderParams = CampaignWithBlazeCardBuilderParams(
    campaign = campaign,
    onCardClick = onCardClick,
    onCampaignClick = onCampaignClick,
    onCreateCampaignClick = onCreateCampaignClick,
    moreMenuParams = CampaignWithBlazeCardBuilderParams.MoreMenuParams(
        viewAllCampaignsItemClick = viewAllCampaignsClick,
        onLearnMoreClick = onLearnMoreItemClick,
        onHideThisCardItemClick = onHideCardMenuItemClick,
        onMoreMenuClick = onMoreMenuClick
    )
)

val moreMenuParams = PromoteWithBlazeCardBuilderParams.MoreMenuParams(
    onLearnMoreClick = onLearnMoreItemClick,
    onHideThisCardItemClick = onHideCardMenuItemClick,
    onMoreMenuClick = onMoreMenuClick
)

val campaignCardItem = BlazeCampaignsCardItem(
    id = campaign.campaignId,
    title = UiString.UiStringText(campaign.title),
    status = CampaignStatus.fromString(campaign.uiStatus),
    featuredImageUrl = campaign.imageUrl,
    stats = BlazeCampaignStats(
        impressions = UiString.UiStringText(campaign.impressions.toString()),
        clicks = UiString.UiStringText(campaign.clicks.toString())
    ),
    onClick = onCampaignClick
)

val blazeCampaignsCardModel = BlazeCampaignsCardModel(
    title = UiString.UiStringRes(R.string.blaze_campaigns_card_title),
    campaign = campaignCardItem,
    footer = BlazeCampaignsCardFooter(
        label = UiString.UiStringRes(R.string.blaze_campaigns_card_footer_label),
        onClick = ListItemInteraction.create(onCreateCampaignClick)
    ),
    onClick = ListItemInteraction.create(onCardClick),
    moreMenuOptions = BlazeCampaignsCardModel.MoreMenuOptions(
        viewAllCampaignsItemClick = ListItemInteraction.create(viewAllCampaignsClick),
        learnMoreClick = ListItemInteraction.create(onLearnMoreItemClick),
        hideThisMenuItemClick = ListItemInteraction.create(onHideCardMenuItemClick),
        onMoreClick = ListItemInteraction.create(onMoreMenuClick)
    )
)


class BlazeCardBuilderTest {
    private lateinit var builder: BlazeCardBuilder

    @Mock
    private var statsUtils: StatsUtils = mock()

    @Before
    fun setUp() {
        builder = BlazeCardBuilder(statsUtils)
    }

    @Test
    fun `when promoteBlazeBuilderParams, then return the PromoteWithBlazeCard`() {
        // Arrange
        val params = PromoteWithBlazeCardBuilderParams(
            onClick = { },
            moreMenuParams = moreMenuParams
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
        assertNotNull(result.moreMenuOptions)
    }

    @Test
    fun `when campaignBuilderParams, then return the CampaignCard`() {
        // Arrange
        val params = campaignWithBlazeBuilderParams
        whenever(statsUtils.toFormattedString(campaign.impressions, ONE_THOUSAND)).thenReturn("1")
        whenever(statsUtils.toFormattedString(campaign.clicks, ONE_THOUSAND)).thenReturn("1")

        // Act
        val result = builder.build(params) as BlazeCampaignsCardModel

        // Assert
        assertEquals(blazeCampaignsCardModel, result)
    }

    @Test
    fun `given campaign not active or completed, when campaign card is built, then no stats available`() {
        // Arrange
        val params = campaignWithBlazeBuilderParams.copy(
            campaign = campaign.copy(uiStatus = "scheduled")
        )

        // Act
        val result = builder.build(params) as BlazeCampaignsCardModel

        // Assert
        val expectedCampaignCard = blazeCampaignsCardModel.copy(
            campaign = campaignCardItem.copy(
                stats = null,
                status = CampaignStatus.Scheduled
            )
        )
        assertEquals(expectedCampaignCard, result)
    }
}
