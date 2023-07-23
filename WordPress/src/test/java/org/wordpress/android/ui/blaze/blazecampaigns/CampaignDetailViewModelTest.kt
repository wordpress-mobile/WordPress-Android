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
import org.mockito.kotlin.mock
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailUIModel
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository

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

    private lateinit var viewModel: CampaignDetailViewModel

    @Before
    fun setup() {
        viewModel = CampaignDetailViewModel(
            accountStore,
            blazeFeatureUtils,
            selectedSiteRepository,
            siteStore
        )

        whenever(blazeFeatureUtils.getUserAgent()).thenReturn("user-agent")
        whenever(blazeFeatureUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn("postdata")

        whenever(accountStore.account).thenReturn(mock())
        whenever(accountStore.account.userName).thenReturn("username")
        whenever(accountStore.accessToken).thenReturn("accessToken")
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(selectedSiteRepository.getSelectedSite()?.url).thenReturn("test.wordpress.com")
        whenever(siteStore.wPComSites).thenReturn(listOf(mock()))
        whenever(siteStore.wPComSites[0].unmappedUrl).thenReturn("")
        whenever(siteStore.wPComSites[0].url).thenReturn("test.wordpress.com")
        whenever(siteStore.wPComSites[0].isAdmin).thenReturn(true)
    }
    @Test
    fun `given valid campaignId and pageSource, when start is called, then trackCampaignDetailsOpened is called`() {
        viewModel.start(1, CampaignDetailPageSource.DASHBOARD_CARD)

        verify(blazeFeatureUtils).trackCampaignDetailsOpened(any())
    }

    @Test
    fun `given invalid account username, when start is called, then FinishActivityWithMessage is posted`() {
        whenever(accountStore.account.userName).thenReturn(null)

        val uiModels = mutableListOf<CampaignDetailUIModel>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiModels) {
            viewModel.start(1, CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(actionEvents.last()).isInstanceOf(BlazeActionEvent.FinishActivityWithMessage::class.java)
        }
    }

    @Test
    fun `given invalid accessToken, when start is called, then FinishActivityWithMessage is posted`() {
        whenever(accountStore.accessToken).thenReturn(null)

        val uiModels = mutableListOf<CampaignDetailUIModel>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiModels) {
            viewModel.start(1, CampaignDetailPageSource.DASHBOARD_CARD)

            assertThat(actionEvents.last()).isInstanceOf(BlazeActionEvent.FinishActivityWithMessage::class.java)
        }
    }
    @Test
    fun `given valid account info, when start is called, then url is built`() {
        val domain = "test.wordpress.com"
        val postData = "postdata"
        whenever(siteStore.wPComSites[0].url).thenReturn(domain)
        whenever(siteStore.wPComSites[0].unmappedUrl).thenReturn("")
        whenever(blazeFeatureUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn(postData)

        val uiModels = mutableListOf<CampaignDetailUIModel>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        testWithData(actionEvents, uiModels) {
            viewModel.start(1, CampaignDetailPageSource.DASHBOARD_CARD)

            val uiModel = uiModels.last()
            assertThat(uiModel.url).isNotEmpty
            assertThat(uiModel.url).contains(domain)
            assertThat(uiModel.addressToLoad).isNotEmpty
            assertThat(uiModel.addressToLoad).contains(postData)
        }
    }
    @Test
    fun `when onRedirectToExternal browser is requested, then launch external browser action event is posted`() {
        val uiModels = mutableListOf<CampaignDetailUIModel>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        val url = "https://external.url.com"

        testWithData(actionEvents, uiModels) {
            viewModel.onRedirectToExternalBrowser(url)

            val actionEvent = actionEvents.last()
            assertThat(actionEvent).isInstanceOf(BlazeActionEvent.LaunchExternalBrowser::class.java)
            assertThat((actionEvent as BlazeActionEvent.LaunchExternalBrowser).url).isEqualTo(url)
        }
    }
    @Test
    fun `when on web view error handled, then finish activity with message event is posted`() {
        val uiModels = mutableListOf<CampaignDetailUIModel>()
        val actionEvents = mutableListOf<BlazeActionEvent>()
        val errorMessageResId = R.string.blaze_campaign_detail_error

        testWithData(actionEvents, uiModels) {
            viewModel.onWebViewError()

            val actionEvent = actionEvents.last()
            assertThat(actionEvent).isInstanceOf(BlazeActionEvent.FinishActivityWithMessage::class.java)
            assertThat((actionEvent as BlazeActionEvent.FinishActivityWithMessage).id).isEqualTo(errorMessageResId)
        }
    }
    private fun testWithData(
        actionEvents: MutableList<BlazeActionEvent> = mutableListOf(),
        uiModels: MutableList<CampaignDetailUIModel> = mutableListOf(),
        testBody: suspend TestScope.() -> Unit
    ) = test {
        val actionEventsJob = launch { viewModel.actionEvents.toList(actionEvents) }
        val uiModelsJob = launch { viewModel.model.toList(uiModels) }
        testBody(testScope())
        actionEventsJob.cancel()
        uiModelsJob.cancel()
    }
}
