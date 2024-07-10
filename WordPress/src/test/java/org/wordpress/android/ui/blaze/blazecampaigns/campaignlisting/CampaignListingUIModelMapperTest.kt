package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CampaignListingUIModelMapperTest : BaseUnitTest() {
    @Mock
    lateinit var statsUtils: StatsUtils

    private lateinit var campaignListingUIModelMapper: CampaignListingUIModelMapper

    @Before
    fun Setup() {
        campaignListingUIModelMapper = CampaignListingUIModelMapper(statsUtils)
    }

    private val activeCampaign = BlazeCampaignModel(
        campaignId = "1",
        title = "title",
        imageUrl = "imageUrl",
        startTime = mock(),
        durationInDays = 1,
        uiStatus = "active",
        impressions = 1,
        clicks = 1,
        targetUrn = null,
        totalBudget = 1.0,
        spentBudget = 0.0,
    )

    @Test
    fun `given campaign model, when mapper is called, then campaign model is created`() {
        whenever(statsUtils.toFormattedString(1L, ONE_THOUSAND)).thenReturn("1")

        val campaigns = campaignListingUIModelMapper.mapToCampaignModels(listOf(activeCampaign))

        assertThat(campaigns).hasSize(1)
        assertThat(campaigns[0].id).isEqualTo("1")
        assertThat(campaigns[0].title).isEqualTo(UiString.UiStringText("title"))
        assertThat(campaigns[0].status).isEqualTo(CampaignStatus.Active)
        assertThat(campaigns[0].featureImageUrl).isEqualTo("imageUrl")
        assertThat(campaigns[0].impressions).isEqualTo(UiString.UiStringText("1"))
        assertThat(campaigns[0].clicks).isEqualTo(UiString.UiStringText("1"))
        assertThat(campaigns[0].budget).isEqualTo(UiString.UiStringText("$1"))
    }

    private val inActiveCampaign = BlazeCampaignModel(
        campaignId = "1",
        title = "title",
        imageUrl = "imageUrl",
        startTime = mock(),
        durationInDays = 1,
        uiStatus = "canceled",
        impressions = 0,
        clicks = 0,
        targetUrn = null,
        totalBudget = 0.0,
        spentBudget = 0.0,
    )

    @Test
    fun `given inactive campaign, when mapper is called, then camapign stats is null`() {
        val campaigns = campaignListingUIModelMapper.mapToCampaignModels(listOf(inActiveCampaign))

        assertThat(campaigns[0].status).isEqualTo(CampaignStatus.Canceled)
        assertThat(campaigns[0].impressions).isNull()
        assertThat(campaigns[0].clicks).isNull()
    }
}
