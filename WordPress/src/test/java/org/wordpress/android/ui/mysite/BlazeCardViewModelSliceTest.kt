package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.FetchCampaignListUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.blaze.MostRecentCampaignUseCase
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BlazeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var blazeCardBuilder: BlazeCardBuilder

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var fetchCampaignListUseCase: FetchCampaignListUseCase

    @Mock
    lateinit var mostRecentCampaignUseCase: MostRecentCampaignUseCase

    private lateinit var blazeCardViewModelSlice: BlazeCardViewModelSlice
    private lateinit var blazeCardViewModelSliceSpy: BlazeCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var refreshActions: MutableList<Boolean>

    private val campaignId = "1"

    private lateinit var uiModel: MutableList<MySiteCardAndItem.Card.BlazeCard>

    @Before
    fun setup() {
        blazeCardViewModelSlice = BlazeCardViewModelSlice(
            testDispatcher(),
            blazeFeatureUtils,
            selectedSiteRepository,
            cardsTracker,
            blazeCardBuilder,
            networkUtilsWrapper,
            fetchCampaignListUseCase,
            mostRecentCampaignUseCase
        )

        blazeCardViewModelSliceSpy = spy(blazeCardViewModelSlice)

        uiModel = mutableListOf()
        navigationActions = mutableListOf()
        refreshActions = mutableListOf()

        blazeCardViewModelSlice.initialize(testScope())
        blazeCardViewModelSliceSpy.initialize(testScope())

        blazeCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        blazeCardViewModelSlice.isRefreshing.observeForever { event ->
            event?.let {
                refreshActions.add(it)
            }
        }

        blazeCardViewModelSlice.uiModel.observeForever { event ->
            event?.let {
                uiModel.add(it)
            }
        }

        blazeCardViewModelSliceSpy.uiModel.observeForever { event ->
            event?.let {
                uiModel.add(it)
            }
        }
    }

    @Test
    fun `given request start, when build card is invoked, then isRefreshing is true`() {
        // When
        blazeCardViewModelSlice.buildCard(site = mock())

        // Then
        assertThat(refreshActions.first()).isTrue
    }

    @Test
    fun `given request finish, when build card is invoked, then isRefreshing is false`() {
        // When
        blazeCardViewModelSlice.buildCard(site = mock())

        // Then
        assertThat(refreshActions.last()).isFalse()
    }

    @Test
    fun `given should show entry point, when build card is invoked, then campaigns are fetched`() = test {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(any())).thenReturn(true)
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(fetchCampaignListUseCase.execute(any(), any(), any())).thenReturn(mock())

        // When
        blazeCardViewModelSlice.buildCard(mock())

        // Then
        verify(fetchCampaignListUseCase).execute(any(), any(), any())
    }

    @Test
    fun `given should show blaze campaigns, when build card is invoked, then campaigns are fetched`() = test {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(any())).thenReturn(true)
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(fetchCampaignListUseCase.execute(any(), any(), any())).thenReturn(mock())

        // When
        blazeCardViewModelSlice.buildCard(mock())

        // Then
        verify(fetchCampaignListUseCase).execute(any(), any(), any())
    }

    @Test
    fun `given no network, when build card is invoked, then most recent campaigns are requested`() = test {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(any())).thenReturn(true)
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(mostRecentCampaignUseCase.execute(any())).thenReturn(mock())

        // When
        blazeCardViewModelSlice.buildCard(mock())

        // Then
        verify(mostRecentCampaignUseCase).execute(any())
    }

    @Test
    fun `given should not show entry point, when build card is invoked, then campaigns are not fetched`() = test {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(any())).thenReturn(false)

        // When
        blazeCardViewModelSlice.buildCard(mock())

        // Then
        verifyNoInteractions(fetchCampaignListUseCase)
        assertThat(uiModel).isEmpty()
    }

    @Test
    fun `given should not show blaze campaigns, when build card is invoked, then promote blaze card is shown`() = test {
        // Arrange
        val promoteWithBlazeCardBuilderParams = mock<PromoteWithBlazeCardBuilderParams>()
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(any())).thenReturn(true)
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(false)
        doReturn(promoteWithBlazeCardBuilderParams)
            .whenever(blazeCardViewModelSliceSpy).getBlazeCardBuilderParams(null)

        val promoteWithBlazeCard = mock<MySiteCardAndItem.Card.BlazeCard.PromoteWithBlazeCard>()
        whenever(blazeCardBuilder.build(promoteWithBlazeCardBuilderParams)).thenReturn(promoteWithBlazeCard)

        // Act
        blazeCardViewModelSliceSpy.buildCard(mock())

        // Assert
        verify(blazeCardViewModelSliceSpy).getBlazeCardBuilderParams(null)
        assertThat(uiModel.first()).isInstanceOf(MySiteCardAndItem.Card.BlazeCard.PromoteWithBlazeCard::class.java)
    }

    @Test
    fun `given campaign model is not null, when params is invoked, then return campaign card builder params`() {
        // Given
        val blazeCampaignModel: BlazeCampaignModel = mock()

        // When
        val result = blazeCardViewModelSlice.getBlazeCardBuilderParams(blazeCampaignModel)

        // Then
        assertThat(result).isInstanceOf(CampaignWithBlazeCardBuilderParams::class.java)
    }

    @Test
    fun `given promote blaze, when params is invoked, then return promote card builder params`() {
        // When
        val result = blazeCardViewModelSlice.getBlazeCardBuilderParams()

        // Then
        assertThat(result).isInstanceOf(PromoteWithBlazeCardBuilderParams::class.java)
    }

    @Test
    fun `given blaze campaign card built, when onCreateCampaignClick invoked, then event is tracked`() = test {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.onCreateCampaignClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
    }

    @Test
    fun `given blaze campaign card built, when campaignClick invoked , then event is tracked`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.onCampaignClick(campaignId)

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
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.onCardClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.DASHBOARD_CARD))
    }

    @Test
    fun `given campaign card built, when more menu clicked, then events are tracked`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.moreMenuParams.onMoreMenuClick()

        // Then
        verify(cardsTracker).trackCardMoreMenuClicked(CardsTracker.Type.BLAZE_CAMPAIGNS.label)
        verify(blazeFeatureUtils).track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_MENU_ACCESSED,
            BlazeFlowSource.DASHBOARD_CARD
        )
    }

    @Test
    fun `given campaign card built, when learn more menu option clicked, then site navigation is triggered`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.moreMenuParams.onLearnMoreClick()

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
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams

        // When
        params.moreMenuParams.viewAllCampaignsItemClick()

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
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams(mock()) as CampaignWithBlazeCardBuilderParams
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())

        // When
        params.moreMenuParams.onHideThisCardItemClick()

        // Then
        verify(blazeFeatureUtils).hideBlazeCard(any())
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.HIDE_THIS.label
        )
        verify(blazeFeatureUtils).track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_HIDE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
    }

    @Test
    fun `given promote blaze card built, when card click invoked, then event is triggered`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams() as PromoteWithBlazeCardBuilderParams

        // When
        params.onClick()

        // Then
        assertThat(navigationActions)
            .containsOnly(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
    }

    @Test
    fun `given promote blaze card built, when hide card invoked, then event is tracked`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams() as PromoteWithBlazeCardBuilderParams
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())

        // When
        params.moreMenuParams.onHideThisCardItemClick()

        // Then
        verify(blazeFeatureUtils).track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_HIDE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        verify(blazeFeatureUtils).hideBlazeCard(any())
    }

    @Test
    fun `given promote blaze card built, when more menu clicked, then event is tracked`() {
        // Given
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams() as PromoteWithBlazeCardBuilderParams

        // When
        params.moreMenuParams.onMoreMenuClick()

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
        val params = blazeCardViewModelSlice.getBlazeCardBuilderParams() as PromoteWithBlazeCardBuilderParams

        // When
        params.moreMenuParams.onLearnMoreClick()

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
}
