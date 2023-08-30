package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BlazeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var cardsTracker: CardsTracker

    private lateinit var blazeCardViewModelSlice: BlazeCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var refreshActions: MutableList<Boolean>

    private val campaignId = 1

    @Before
    fun setup() {
        blazeCardViewModelSlice = BlazeCardViewModelSlice(blazeFeatureUtils, selectedSiteRepository, cardsTracker)

        navigationActions = mutableListOf()
        refreshActions = mutableListOf()

        blazeCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        blazeCardViewModelSlice.refresh.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                refreshActions.add(it)
            }
        }
    }

    @Test
    fun `given site blaze eligible, when isSiteBlazeEligible is called, then return true`() {
        // Given
        val site: SiteModel = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(true)

        // When
        val result = blazeCardViewModelSlice.isSiteBlazeEligible()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `given site blaze ineligible, when isSiteBlazeEligible is called, then return false`() {
        // Given
        val site: SiteModel = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)

        // When
        val result = blazeCardViewModelSlice.isSiteBlazeEligible()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `given campaign blaze card update, when params is invoked, then return campaign card builder params`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result = blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate)

        // Then
        assertThat(result).isInstanceOf(CampaignWithBlazeCardBuilderParams::class.java)
    }

    @Test
    fun `given promote blaze card update, when params is invoked, then return promote card builder params`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)

        // When
        val result = blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate)

        // Then
        assertThat(result).isInstanceOf(PromoteWithBlazeCardBuilderParams::class.java)
    }

    @Test
    fun `given blaze card in eligible, when params is invoked, then return null`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(false)

        // When
        val result = blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `given blaze campaign card built, when onCreateCampaignClick invoked , then event is tracked`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.onCreateCampaignClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
    }

    @Test
    fun `given blaze campaign card built, when campaignClick invoked , then event is tracked`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.onCampaignClick(campaignId)

        // Then
        assertThat(navigationActions)
            .containsOnly(
                SiteNavigationAction.OpenCampaignDetailPage(
                    campaignId,
                    CampaignDetailPageSource.DASHBOARD_CARD
                )
            )
    }

    @Test
    fun `given blaze campaign card built, when card click invoked , then event is tracked`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.onCardClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.DASHBOARD_CARD))
    }

    @Test
    fun `given campaign card built, when learn more menu option clicked, then site navigation is triggered`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.moreMenuParams.onLearnMoreClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(
                SiteNavigationAction.OpenPromoteWithBlazeOverlay(
                    source = BlazeFlowSource.DASHBOARD_CARD,
                    shouldShowBlazeOverlay = true
                )
            )
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.LEARN_MORE.label
        )
    }

    @Test
    fun `given campaign card built, when view all campaigns menu option clicked, then site navigation is triggered`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.moreMenuParams.viewAllCampaignsItemClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.DASHBOARD_CARD))
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.VIEW_ALL_CAMPAIGNS.label
        )
    }

    @Test
    fun `given campaign card built, when hide campaigns menu option clicked, then site navigation is triggered`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeCardUpdate.campaign).thenReturn(mock())

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as CampaignWithBlazeCardBuilderParams
        result.moreMenuParams.onHideThisCardItemClick()

        // Then
        verify(blazeFeatureUtils).hideBlazeCard(any())
    }

    @Test
    fun `given promote blaze card built, when card click invoked, then event is triggered`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as PromoteWithBlazeCardBuilderParams
        result.onClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
    }

    @Test
    fun `given promote blaze card built, when hide card invoked, then event is tracked`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as PromoteWithBlazeCardBuilderParams
        result.moreMenuParams.onHideThisCardItemClick()

        // Then
        verify(blazeFeatureUtils).track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_HIDE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        verify(blazeFeatureUtils).hideBlazeCard(any())
        assertThat(refreshActions).containsOnly(true)
    }

    @Test
    fun `given promote blaze card built, when more menu clicked, then event is tracked`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(null)

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as PromoteWithBlazeCardBuilderParams
        result.moreMenuParams.onMoreMenuClick()

        // Then
        verify(blazeFeatureUtils).track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_MENU_ACCESSED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        verify(cardsTracker)
            .trackCardMoreMenuClicked(CardsTracker.Type.PROMOTE_WITH_BLAZE.label)
    }

    @Test
    fun `given promote blaze card built, when learn more menu option clicked, then site navigation is triggered`() {
        // Given
        val blazeCardUpdate: MySiteUiState.PartialState.BlazeCardUpdate = mock()
        whenever(blazeCardUpdate.blazeEligible).thenReturn(true)
        whenever(blazeCardUpdate.campaign).thenReturn(null)

        // When
        val result =
            blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCardUpdate) as PromoteWithBlazeCardBuilderParams
        result.moreMenuParams.onLearnMoreClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(
                SiteNavigationAction.OpenPromoteWithBlazeOverlay(
                    source = BlazeFlowSource.DASHBOARD_CARD,
                    shouldShowBlazeOverlay = true
                )
            )
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.PROMOTE_WITH_BLAZE.label,
            PromoteWithBlazeCardMenuItem.LEARN_MORE.label
        )
    }

    @Test
    fun `given campaigns enabled, when menu clicked, then navigated to campaign listing page`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)

        // When
        val navigationAction = blazeCardViewModelSlice.onBlazeMenuItemClick()

        // Then
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.MENU_ITEM)
        assertEquals(
            SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM),
            navigationAction
        )
    }

    @Test
    fun `given campaigns disabled, when menu clicked, then event is tracked`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(false)

        // When
        val navigationAction = blazeCardViewModelSlice.onBlazeMenuItemClick()

        // Then
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.MENU_ITEM)
        assertEquals(
            SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.MENU_ITEM),
            navigationAction
        )
    }
}
