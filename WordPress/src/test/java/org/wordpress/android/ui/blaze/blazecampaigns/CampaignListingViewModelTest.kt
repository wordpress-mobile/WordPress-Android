package org.wordpress.android.ui.blaze.blazecampaigns

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingUIModelMapper
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingNavigation
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingUiState
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CampaignListingViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CampaignListingViewModel

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var blazeCampaignsStore: BlazeCampaignsStore

    @Mock
    lateinit var mapper: CampaignListingUIModelMapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var resourceProvider: ResourceProvider

    private val uiStates = mutableListOf<CampaignListingUiState>()

    private val navigationEvents = mutableListOf<CampaignListingNavigation>()

    lateinit var campaignModel: BlazeCampaignsModel

    @Before
    fun setUp() {
        viewModel = CampaignListingViewModel(
            testDispatcher(),
            blazeFeatureUtils,
            blazeCampaignsStore,
            selectedSiteRepository,
            networkUtilsWrapper,
            resourceProvider,
            mapper
        )
        observeUIState()
        observeNavigationEvents()
        setupCampaignModels()
    }

    private fun observeUIState() {
        viewModel.uiState.observeForever {
            it?.let { uiStates.add(it) }
        }
    }

    private fun observeNavigationEvents() {
        viewModel.navigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationEvents.add(it)
            }
        }
    }

    @Test
    fun `when viewmodel start, then should track campaign listing page shown`() {
        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)

        assertThat(uiStates.first() is CampaignListingUiState.Loading).isTrue
        verify(blazeFeatureUtils).trackCampaignListingPageShown(CampaignListingPageSource.DASHBOARD_CARD)
    }

    @Test
    fun `given internet available, when viewmodel start, then should fetch campaigns`() = runTest {
        val siteModel = mock<SiteModel>()
        setupCampaignModels()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.getBlazeCampaigns(siteModel)).thenReturn(campaignModel)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        advanceUntilIdle()

        assertThat(uiStates.last() is CampaignListingUiState.Success).isTrue
        assertThat((uiStates.last() as CampaignListingUiState.Success).campaigns.size).isEqualTo(10)
    }

    @Test
    fun `given no campaigns, when viewmodel start, then should show no campaigns error`() = runTest {
        val siteModel = mock<SiteModel>()
        val noCampaigns = BlazeCampaignsModel(emptyList(), 1, 1, 0)

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.getBlazeCampaigns(siteModel)).thenReturn(noCampaigns)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        advanceUntilIdle()

        assertThat(uiStates.last() is CampaignListingUiState.Error).isTrue
    }


    @Test
    fun `given no campaigns, when click is invoked on create, then navigate to blaze flow`() = runTest {
        val siteModel = mock<SiteModel>()
        val noCampaigns = BlazeCampaignsModel(emptyList(), 1, 1, 0)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.getBlazeCampaigns(siteModel)).thenReturn(noCampaigns)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        val noCampaignsState = uiStates.last() as CampaignListingUiState.Error
        noCampaignsState.button!!.click.invoke()

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignCreatePage).isTrue
    }

    @Test
    fun `given campaigns available, when clicked on campaign, then navigate to detail`() = runTest {
        val siteModel = mock<SiteModel>()
        setupCampaignModels()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.getBlazeCampaigns(siteModel)).thenReturn(campaignModel)


        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        val success = uiStates.last() as CampaignListingUiState.Success
        success.itemClick.invoke(success.campaigns.first())

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignDetailPage).isTrue
    }

    @Test
    fun `given campaigns available, when clicked on create campaign fab, then navigate to blaze flow`() = runTest {
        val siteModel = mock<SiteModel>()
        setupCampaignModels()
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.getBlazeCampaigns(siteModel)).thenReturn(campaignModel)


        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        val success = uiStates.last() as CampaignListingUiState.Success
        success.createCampaignClick.invoke()

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignCreatePage).isTrue
    }

    private fun setupCampaignModels() {
        campaignModel = BlazeCampaignsModel(
            campaigns = getCampaigns(10),
            page = 1,
            totalPages = 1,
            totalItems = 10
        )
    }

    private fun getCampaigns(numbers: Int): List<BlazeCampaignModel> {
        val listOfCampaigns = mutableListOf<BlazeCampaignModel>()
        for (i in 0 until numbers) {
            listOfCampaigns.add(
                BlazeCampaignModel(
                    campaignId = i,
                    title = "Campaign $i",
                    imageUrl = "https://picsum.photos/200/300",
                    startDate = Date(),
                    endDate = Date(),
                    uiStatus = CampaignStatus.Active.status,
                    budgetCents = 1000L,
                    impressions = 1000L,
                    clicks = 1000L,
                )
            )
        }
        return listOfCampaigns
    }
}
