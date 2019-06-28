package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel.SiteUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.WidgetAdded
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.WidgetSettingsModel
import org.wordpress.android.viewmodel.Event

class StatsWidgetConfigureViewModelTest : BaseUnitTest() {
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    @Mock private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var viewModel: StatsWidgetConfigureViewModel
    private val selectedSite = MutableLiveData<SiteUiModel>()
    private val viewMode = MutableLiveData<Color>()
    private val siteId = 15L
    private val siteName = "WordPress"
    private val siteUrl = "wordpress.com"
    private val iconUrl = "icon.jpg"
    private val viewType = WidgetType.WEEK_VIEWS
    @Before
    fun setUp() {
        viewModel = StatsWidgetConfigureViewModel(Dispatchers.Unconfined, appPrefsWrapper)
        whenever(siteSelectionViewModel.selectedSite).thenReturn(selectedSite)
        whenever(colorSelectionViewModel.viewMode).thenReturn(viewMode)
        viewMode.value = DARK
    }

    @Test
    fun `loads site and view mode from app prefs on start`() {
        val appWidgetId = 10

        viewModel.start(appWidgetId, viewType, siteSelectionViewModel, colorSelectionViewModel)

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }

        selectedSite.value = SiteUiModel(siteId, iconUrl, siteName, siteUrl) {}

        assertThat(settingsModel).isNotNull
        assertThat(settingsModel!!.buttonEnabled).isTrue()
        assertThat(settingsModel!!.siteTitle).isEqualTo(siteName)
        assertThat(settingsModel!!.color).isEqualTo(DARK)
    }

    @Test
    fun `button is disabled when site not set`() {
        val appWidgetId = 10

        viewModel.start(appWidgetId, viewType, siteSelectionViewModel, colorSelectionViewModel)

        var settingsModel: WidgetSettingsModel? = null
        viewModel.settingsModel.observeForever {
            settingsModel = it
        }

        assertThat(settingsModel).isNotNull
        assertThat(settingsModel!!.buttonEnabled).isFalse()
        assertThat(settingsModel!!.siteTitle).isNull()
        assertThat(settingsModel!!.color).isEqualTo(DARK)
    }

    @Test
    fun `on add clicked sets up widget on started widget`() {
        val appWidgetId = 10

        viewModel.start(appWidgetId, viewType, siteSelectionViewModel, colorSelectionViewModel)

        selectedSite.value = SiteUiModel(siteId, iconUrl, siteName, siteUrl) {}

        var event: Event<WidgetAdded>? = null
        viewModel.widgetAdded.observeForever { event = it }

        viewModel.addWidget()

        verify(appPrefsWrapper).setAppWidgetSiteId(siteId, appWidgetId)
        verify(appPrefsWrapper).setAppWidgetColor(DARK, appWidgetId)

        val widgetAdded: WidgetAdded? = event?.getContentIfNotHandled()
        assertThat(widgetAdded).isNotNull
        assertThat(widgetAdded!!.widgetType).isEqualTo(viewType)
        assertThat(widgetAdded.appWidgetId).isEqualTo(appWidgetId)
    }
}
