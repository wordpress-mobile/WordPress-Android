package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel.SiteUiModel
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class StatsSiteSelectionViewModelTest : BaseUnitTest() {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var wpComSite: SiteModel
    private lateinit var jetpackSite: SiteModel
    private lateinit var nonJetpackSite: SiteModel
    private lateinit var viewModel: StatsSiteSelectionViewModel
    private val siteId = 15L
    private val siteName = "WordPress"
    private val jetpackSiteName = "Jetpack"
    private val nonJetpackSiteName = "Non-Jetpack"
    private val siteUrl = "wordpress.com"
    private val iconUrl = "icon.jpg"

    @Before
    fun setUp() {
        viewModel = StatsSiteSelectionViewModel(
                testDispatcher(),
                siteStore,
                accountStore,
                appPrefsWrapper
        )
        wpComSite = SiteModel()
        wpComSite.siteId = siteId
        wpComSite.name = siteName
        wpComSite.url = siteUrl
        wpComSite.iconUrl = iconUrl
        wpComSite.setIsJetpackConnected(false)
        wpComSite.setIsWPCom(true)
        jetpackSite = SiteModel()
        jetpackSite.siteId = siteId
        jetpackSite.name = jetpackSiteName
        jetpackSite.url = siteUrl
        jetpackSite.iconUrl = iconUrl
        jetpackSite.setIsJetpackConnected(true)
        jetpackSite.setIsWPCom(false)
        nonJetpackSite = SiteModel()
        nonJetpackSite.siteId = siteId
        nonJetpackSite.name = nonJetpackSiteName
        nonJetpackSite.url = siteUrl
        nonJetpackSite.iconUrl = iconUrl
        nonJetpackSite.setIsJetpackConnected(false)
        nonJetpackSite.setIsWPCom(false)
    }

    @Test
    fun `loads sites`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(wpComSite))

        viewModel.loadSites()

        assertThat(sites).isNotNull
        assertThat(sites).hasSize(1)
        val loadedSite = sites!![0]
        assertThat(loadedSite.iconUrl).isEqualTo(iconUrl)
        assertThat(loadedSite.siteId).isEqualTo(siteId)
        assertThat(loadedSite.title).isEqualTo(siteName)
        assertThat(loadedSite.url).isEqualTo(siteUrl)
    }

    @Test
    fun `filters out non-jetpack self-hosted sites`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(jetpackSite, wpComSite, nonJetpackSite))

        viewModel.loadSites()

        assertThat(sites).isNotNull
        assertThat(sites).hasSize(2)
        val jetpackSite = sites!![0]
        assertThat(jetpackSite.title).isEqualTo(jetpackSiteName)
        val wpComSite = sites!![1]
        assertThat(wpComSite.title).isEqualTo(siteName)
    }

    @Test
    fun `hides dialog and selects site on site click`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(wpComSite))

        viewModel.loadSites()

        assertThat(sites).isNotNull
        assertThat(sites).hasSize(1)
        val loadedSite = sites!![0]

        var hideSiteDialog: Unit? = null
        viewModel.hideSiteDialog.observeForever { hideSiteDialog = it?.getContentIfNotHandled() }

        loadedSite.click()

        assertThat(hideSiteDialog).isNotNull
    }

    @Test
    fun `opens dialog when access token present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.dialogOpened.observeForever {
            event = it
        }

        viewModel.openSiteDialog()

        assertThat(event).isNotNull
    }

    @Test
    fun `shows notification when access token not present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        var notification: Event<Int>? = null
        viewModel.notification.observeForever {
            notification = it
        }

        viewModel.openSiteDialog()

        assertThat(notification).isNotNull
        assertThat(notification?.getContentIfNotHandled()).isEqualTo(R.string.stats_widget_log_in_message)
    }
}
