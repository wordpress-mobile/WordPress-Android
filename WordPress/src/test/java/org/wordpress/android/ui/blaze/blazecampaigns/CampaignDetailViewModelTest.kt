package org.wordpress.android.ui.blaze.blazecampaigns

import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var actionEvents: MutableList<BlazeActionEvent>
    private lateinit var model: MutableList<CampaignDetailUIModel>

    @Before
    fun setup() {
        viewModel = CampaignDetailViewModel(
            accountStore,
            blazeFeatureUtils,
            selectedSiteRepository,
            siteStore
        )
        actionEvents = mutableListOf()
        model = mutableListOf()

        whenever(blazeFeatureUtils.getUserAgent()).thenReturn("user-agent")
        whenever(blazeFeatureUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn("postdata")

        whenever(accountStore.account).thenReturn(mock())
        whenever(accountStore.account.userName).thenReturn("username")
        whenever(accountStore.accessToken).thenReturn("accessToken")
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(selectedSiteRepository.getSelectedSite()?.url).thenReturn("url")
        whenever(siteStore.wPComSites).thenReturn(listOf(mock()))
        whenever(siteStore.wPComSites[0].unmappedUrl).thenReturn("unmappedUrl")
        whenever(siteStore.wPComSites[0].url).thenReturn("test.wordpress.com")
        whenever(siteStore.wPComSites[0].url).thenReturn("test.wordpress.com")

   }
    @Test
    fun `given valid campaignId and pageSource, when start is called, then trackCampaignDetailsOpened is called`() {
        viewModel.start(1, CampaignDetailPageSource.DASHBOARD_CARD)

        verify(blazeFeatureUtils).trackCampaignDetailsOpened(any())
    }
}
