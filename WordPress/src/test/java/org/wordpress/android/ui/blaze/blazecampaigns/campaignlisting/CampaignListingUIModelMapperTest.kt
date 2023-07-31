package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CampaignListingUIModelMapperTest {
    @Mock
    lateinit var statsUtils: StatsUtils

    private lateinit var campaignListingUIModelMapper: CampaignListingUIModelMapper

    @Before
    fun Setup() {
        campaignListingUIModelMapper = CampaignListingUIModelMapper(statsUtils)
    }

    @Test
    fun `given campaign model, when mapper is called, then campaign model is created`() {
        val campaignModel = campaignListingUIModelMapper.mapToCampaignModel(
            BlazeCampaignModel(
                campaignId = 1,
                title = "title",
                uiStatus = "active",
                imageUrl = "imageUrl",
                impressions = 1,
                clicks = 1,
                budgetCents = 1,
                startDate = mock(),
                endDate = mock(),
            )
        )

        assertThat(campaignModel.id).isEqualTo("1")
        assertThat(campaignModel.title).isEqualTo(UiString.UiStringText("title"))
        assertThat(campaignModel.status).isEqualTo(CampaignStatus.Active)
        assertThat(campaignModel.featureImageUrl).isEqualTo("imageUrl")
        assertThat(campaignModel.impressions).isEqualTo(UiString.UiStringText("1"))
        assertThat(campaignModel.clicks).isEqualTo(UiString.UiStringText("1"))
        assertThat(campaignModel.budget).isEqualTo(UiString.UiStringText("$1"))
    }

    @Test
    fun `given inactive campaign, when mapper is called, then camapign stats is null`() {
        val campaignModel = campaignListingUIModelMapper.mapToCampaignModel(
            BlazeCampaignModel(
                campaignId = 1,
                title = "title",
                uiStatus = "canceled",
                imageUrl = "imageUrl",
                impressions = 0,
                clicks = 0,
                budgetCents = 1,
                startDate = mock(),
                endDate = mock(),
            )
        )

        assertThat(campaignModel.status).isEqualTo(CampaignStatus.Canceled)
        assertThat(campaignModel.impressions).isNull()
        assertThat(campaignModel.clicks).isNull()
    }
}
