package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel.SiteUiModel
import org.wordpress.android.viewmodel.Event

class StatsSiteSelectionViewModelTest : BaseUnitTest() {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var site: SiteModel
    private lateinit var viewModel: StatsSiteSelectionViewModel
    private val siteId = 15L
    private val siteName = "WordPress"
    private val siteUrl = "wordpress.com"
    private val iconUrl = "icon.jpg"
    @Before
    fun setUp() {
        viewModel = StatsSiteSelectionViewModel(Dispatchers.Unconfined, siteStore, accountStore, appPrefsWrapper)
        whenever(site.siteId).thenReturn(siteId)
        whenever(site.name).thenReturn(siteName)
        whenever(site.url).thenReturn(siteUrl)
        whenever(site.iconUrl).thenReturn(iconUrl)
    }
    @Test
    fun `loads sites`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(site))

        viewModel.loadSites()

        Assertions.assertThat(sites).isNotNull
        Assertions.assertThat(sites).hasSize(1)
        val loadedSite = sites!![0]
        Assertions.assertThat(loadedSite.iconUrl).isEqualTo(iconUrl)
        Assertions.assertThat(loadedSite.siteId).isEqualTo(siteId)
        Assertions.assertThat(loadedSite.title).isEqualTo(siteName)
        Assertions.assertThat(loadedSite.url).isEqualTo(siteUrl)
    }

    @Test
    fun `hides dialog and selects site on site click`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(site))

        viewModel.loadSites()

        Assertions.assertThat(sites).isNotNull
        Assertions.assertThat(sites).hasSize(1)
        val loadedSite = sites!![0]

        var hideSiteDialog: Unit? = null
        viewModel.hideSiteDialog.observeForever { hideSiteDialog = it?.getContentIfNotHandled() }

        loadedSite.click()

        Assertions.assertThat(hideSiteDialog).isNotNull
    }

    @Test
    fun `opens dialog when access token present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.dialogOpened.observeForever {
            event = it
        }

        viewModel.openSiteDialog()

        Assertions.assertThat(event).isNotNull
    }

    @Test
    fun `shows notification when access token not present`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        var notification: Event<Int>? = null
        viewModel.notification.observeForever {
            notification = it
        }

        viewModel.openSiteDialog()

        Assertions.assertThat(notification).isNotNull
        Assertions.assertThat(notification?.getContentIfNotHandled()).isEqualTo(R.string.stats_widget_log_in_message)
    }
}
