package org.wordpress.android.ui.stats.refresh.lists.widget

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.SiteUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.ViewMode
import org.wordpress.android.ui.stats.refresh.lists.widget.ViewsWidgetViewModel.WidgetSettingsModel

class ViewsWidgetViewModelTest : BaseUnitTest() {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var site: SiteModel
    private lateinit var viewModel: ViewsWidgetViewModel
    private val siteId = 15L
    private val siteName = "WordPress"
    private val siteUrl = "wordpress.com"
    private val iconUrl = "icon.jpg"
    @Before
    fun setUp() {
        viewModel = ViewsWidgetViewModel(Dispatchers.Unconfined, siteStore, appPrefsWrapper)
        whenever(site.siteId).thenReturn(siteId)
        whenever(site.name).thenReturn(siteName)
        whenever(site.url).thenReturn(siteUrl)
        whenever(site.iconUrl).thenReturn(iconUrl)
    }

    @Test
    fun `loads site and view mode from app prefs on start`() {
        val appWidgetId = 10
        whenever(appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)).thenReturn(ViewMode.DARK.ordinal)
        whenever(appPrefsWrapper.getAppWidgetSiteId(appWidgetId)).thenReturn(siteId)
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }

        viewModel.start(appWidgetId)

        assertThat(settingsModel).isNotNull
        assertThat(settingsModel!!.buttonEnabled).isTrue()
        assertThat(settingsModel!!.siteTitle).isEqualTo(siteName)
        assertThat(settingsModel!!.viewMode).isEqualTo(ViewMode.DARK)
    }

    @Test
    fun `button is disabled when view mode not set`() {
        val appWidgetId = 10
        whenever(appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)).thenReturn(-1)
        whenever(appPrefsWrapper.getAppWidgetSiteId(appWidgetId)).thenReturn(siteId)
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }

        viewModel.start(appWidgetId)

        assertThat(settingsModel).isNotNull
        assertThat(settingsModel!!.buttonEnabled).isFalse()
        assertThat(settingsModel!!.siteTitle).isEqualTo(siteName)
        assertThat(settingsModel!!.viewMode).isNull()
    }

    @Test
    fun `button is disabled when site not set`() {
        val appWidgetId = 10
        whenever(appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)).thenReturn(ViewMode.DARK.ordinal)
        whenever(appPrefsWrapper.getAppWidgetSiteId(appWidgetId)).thenReturn(-1)

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }

        viewModel.start(appWidgetId)

        assertThat(settingsModel).isNotNull
        assertThat(settingsModel!!.buttonEnabled).isFalse()
        assertThat(settingsModel!!.siteTitle).isNull()
        assertThat(settingsModel!!.viewMode).isEqualTo(ViewMode.DARK)
    }

    @Test
    fun `loads sites`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(site))

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
    fun `hides dialog and selects site on site click`() {
        var sites: List<SiteUiModel>? = null
        viewModel.sites.observeForever { sites = it }

        whenever(siteStore.sites).thenReturn(listOf(site))

        viewModel.loadSites()

        assertThat(sites).isNotNull
        assertThat(sites).hasSize(1)
        val loadedSite = sites!![0]

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }
        var hideSite: SiteUiModel? = null
        viewModel.hideSite.observeForever { hideSite = it?.getContentIfNotHandled() }

        loadedSite.click()

        assertThat(settingsModel!!.siteTitle).isEqualTo(siteName)
        assertThat(hideSite).isNotNull
    }
}
