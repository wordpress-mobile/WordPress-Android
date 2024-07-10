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
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingNavigation
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingUiState
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingViewModel
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignModel
import org.wordpress.android.Result
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.FetchCampaignListUseCase
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.GetCampaignListFromDbUseCase
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.NoCampaigns
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CampaignListingViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CampaignListingViewModel

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var fetchCampaignListUseCase: FetchCampaignListUseCase

    @Mock
    lateinit var getCampaignListFromDbUseCase: GetCampaignListFromDbUseCase

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var siteModel: SiteModel

    private val uiStates = mutableListOf<CampaignListingUiState>()

    private val navigationEvents = mutableListOf<CampaignListingNavigation>()

    @Before
    fun setUp() {
        viewModel = CampaignListingViewModel(
            testDispatcher(),
            blazeFeatureUtils,
            selectedSiteRepository,
            networkUtilsWrapper,
            resourceProvider,
            fetchCampaignListUseCase,
            getCampaignListFromDbUseCase
        )
        observeUIState()
        observeNavigationEvents()
        mockSiteModel()
    }

    private fun mockSiteModel() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
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
    fun `when viewmodel start, then should fetch campaigns`() = runTest {
        val campaignFetchResult: Result<NoCampaigns, List<CampaignModel>> = Result.Success(mock())
        whenever(getCampaignListFromDbUseCase.execute(siteModel)).thenReturn(campaignFetchResult)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        advanceUntilIdle()

        assertThat(uiStates.last() is CampaignListingUiState.Success).isTrue
    }

    @Test
    fun `given no campaigns in db + api, when viewmodel start, then should show no campaigns error`() = runTest {
        val noCampaigns = Result.Failure(NoCampaigns)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(getCampaignListFromDbUseCase.execute(siteModel)).thenReturn(noCampaigns)
        whenever(fetchCampaignListUseCase.execute(siteModel, 0)).thenReturn(noCampaigns)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        advanceUntilIdle()

        assertThat(uiStates.last() is CampaignListingUiState.Error.NoCampaignsError)
    }


    @Test
    fun `given no campaigns in db + api, when click is invoked on create, then navigate to blaze flow`() = runTest {
        val noCampaigns = Result.Failure(NoCampaigns)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(getCampaignListFromDbUseCase.execute(siteModel)).thenReturn(noCampaigns)
        whenever(fetchCampaignListUseCase.execute(siteModel, offset = 0)).thenReturn(noCampaigns)

        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        advanceUntilIdle()
        val noCampaignsState = uiStates.last() as CampaignListingUiState.Error
        noCampaignsState.button!!.click.invoke()

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignCreatePage).isTrue
    }

    @Test
    fun `given campaigns available, when clicked on campaign, then navigate to detail`() = runTest {
        val campaignFetchResult: Result<NoCampaigns, List<CampaignModel>> = Result.Success(getCampaigns(10))
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(getCampaignListFromDbUseCase.execute(siteModel)).thenReturn(campaignFetchResult)


        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        val success = uiStates.last() as CampaignListingUiState.Success
        success.itemClick.invoke(success.campaigns.first())

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignDetailPage).isTrue
    }

    @Test
    fun `given campaigns available, when clicked on create campaign fab, then navigate to blaze flow`() = runTest {
        val campaigns: Result<NoCampaigns, List<CampaignModel>> = Result.Success(mock())
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(getCampaignListFromDbUseCase.execute(siteModel)).thenReturn(campaigns)


        viewModel.start(CampaignListingPageSource.DASHBOARD_CARD)
        val success = uiStates.last() as CampaignListingUiState.Success
        success.createCampaignClick.invoke()

        assertThat(navigationEvents.last() is CampaignListingNavigation.CampaignCreatePage).isTrue
    }


    private fun getCampaigns(numbers: Int): MutableList<CampaignModel> {
        val listOfCampaigns = mutableListOf<CampaignModel>()
        for (i in 0 until numbers) {
            listOfCampaigns.add(
                CampaignModel(
                    id = i.toString(),
                    title = UiString.UiStringText("Campaign $i"),
                    featureImageUrl = "https://picsum.photos/200/300",
                    status = CampaignStatus.Active,
                    budget = UiString.UiStringText("5$"),
                    impressions = UiString.UiStringText("1000"),
                    clicks = UiString.UiStringText("100"),
                )
            )
        }
        return listOfCampaigns
    }
}
