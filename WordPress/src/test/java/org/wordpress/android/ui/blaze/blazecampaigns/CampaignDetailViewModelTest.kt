package org.wordpress.android.ui.blaze.blazecampaigns

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailMapper
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailUiState
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CampaignDetailViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var mapper: CampaignDetailMapper

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: CampaignDetailViewModel

    @Before
    fun setup() {
        viewModel = CampaignDetailViewModel(
            accountStore,
            blazeFeatureUtils,
            selectedSiteRepository,
            siteStore,
            mapper,
            networkUtilsWrapper,
            testDispatcher(),
        )

        whenever(blazeFeatureUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn("postdata")

        whenever(accountStore.account).thenReturn(mock())
        whenever(accountStore.account.userName).thenReturn("username")
        whenever(accountStore.accessToken).thenReturn("accessToken")
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(selectedSiteRepository.getSelectedSite()?.url).thenReturn("test.wordpress.com")

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(mapper.toGenericError(any())).thenReturn(mock())
        whenever(mapper.toNoNetworkError(any())).thenReturn(mock())
        whenever(mapper.toPrepared(any(), any())).thenReturn(mock())
    }
    @Test
    fun `given valid campaignId and pageSource, when start is called, then trackCampaignDetailsOpened is called`() {
        viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

        verify(blazeFeatureUtils).trackCampaignDetailsOpened(any())
    }

    @Test
    fun `given invalid account username, when start is called, then error state is posted`() {
        whenever(accountStore.account.userName).thenReturn(null)

        val uiState = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiState) {
            viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(uiState.last()).isInstanceOf(CampaignDetailUiState.GenericError::class.java)
        }
    }

    @Test
    fun `given invalid accessToken, when start is called, then error state is posted`() {
        whenever(accountStore.accessToken).thenReturn(null)

        val uiState = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiState) {
            viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(uiState.last()).isInstanceOf(CampaignDetailUiState.GenericError::class.java)
        }
    }
    @Test
    fun `given valid account info, when start is called, then preparing is posted`() {
        whenever(
            blazeFeatureUtils.getAuthenticationPostData(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn("postdata")

        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiStates) {
            viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(uiStates.last()).isInstanceOf(CampaignDetailUiState.Prepared::class.java)
        }
    }

    @Test
    fun `when start is called, then preparing is posted`() {
        whenever(blazeFeatureUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn(any())

        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiStates) {
            viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(uiStates.first()).isInstanceOf(CampaignDetailUiState.Preparing::class.java)
        }
    }

    @Test
    fun `when onRedirectToExternal browser is requested, then launch external browser action event is posted`() {
        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        val url = "https://external.url.com"

        testWithData(actionEvents, uiStates) {
            viewModel.onRedirectToExternalBrowser(url)

            val actionEvent = actionEvents.last()
            assertThat(actionEvent).isInstanceOf(BlazeActionEvent.LaunchExternalBrowser::class.java)
            assertThat((actionEvent as BlazeActionEvent.LaunchExternalBrowser).url).isEqualTo(url)
        }
    }

    @Test
    fun `when on web view error handled, then error view is posted`() {
        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()

        testWithData(actionEvents, uiStates) {
            viewModel.onWebViewError()

            val uiState = uiStates.last()
            assertThat(uiState).isInstanceOf(CampaignDetailUiState.GenericError::class.java)
        }
    }

    @Test
    fun `when no network connection, then error view is posted`() {
        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        testWithData(actionEvents, uiStates) {
            viewModel.start(campaignId = "1", CampaignDetailPageSource.DASHBOARD_CARD)

            val uiState = uiStates.last()
            assertThat(uiState).isInstanceOf(CampaignDetailUiState.NoNetworkError::class.java)
        }
    }

    @Test
    fun `when on page loaded handled, then loaded is posted`() {
        val uiStates = mutableListOf<CampaignDetailUiState>()
        val actionEvents = mutableListOf<BlazeActionEvent>()

        testWithData(actionEvents, uiStates) {
            viewModel.onUrlLoaded()

            val uiState = uiStates.last()
            assertThat(uiState).isInstanceOf(CampaignDetailUiState.Loaded::class.java)
        }
    }

    private fun testWithData(
        actionEvents: MutableList<BlazeActionEvent> = mutableListOf(),
        uiState: MutableList<CampaignDetailUiState> = mutableListOf(),
        testBody: suspend TestScope.() -> Unit
    ) = test {
        val actionEventsJob = launch { viewModel.actionEvents.toList(actionEvents) }
        val uiStatesJob = launch { viewModel.uiState.toList(uiState) }
        testBody(testScope())
        actionEventsJob.cancel()
        uiStatesJob.cancel()
    }
}
