package org.wordpress.android.ui.stats.refresh

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.test
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.STATS
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.StatsViewModel.StatsModuleUiModel
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.NewsCardHandler
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardTodaysStatsCardFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

class StatsViewModelTest : BaseUnitTest() {
    @Mock lateinit var baseListUseCase: BaseListUseCase
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var statsStore: StatsStore
    @Mock lateinit var newsCardHandler: NewsCardHandler
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsModuleActivateUseCase: StatsModuleActivateUseCase
    @Mock lateinit var notificationsTracker: SystemNotificationsTracker
    @Mock lateinit var todaysStatsCardFeatureConfig: MySiteDashboardTodaysStatsCardFeatureConfig
    @Mock lateinit var jetpackBrandingUtils: JetpackBrandingUtils
    @Mock lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
    private lateinit var viewModel: StatsViewModel
    private val _liveSelectedSection = MutableLiveData<StatsSection>()
    private val liveSelectedSection: LiveData<StatsSection> = _liveSelectedSection
    @Before
    fun setUp() {
        whenever(baseListUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(statsSectionManager.liveSelectedSection).thenReturn(liveSelectedSection)
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        viewModel = StatsViewModel(
                mapOf(DAYS to baseListUseCase),
                Dispatchers.Unconfined,
                Dispatchers.Default,
                selectedDateProvider,
                statsSectionManager,
                analyticsTracker,
                networkUtilsWrapper,
                statsSiteProvider,
                statsStore,
                newsCardHandler,
                statsModuleActivateUseCase,
                notificationsTracker,
                todaysStatsCardFeatureConfig,
                jetpackBrandingUtils,
                jetpackFeatureRemovalOverlayUtil
        )

        viewModel.start(1, false, null, null, false, null)
    }

    @Test
    fun `stores and tracks tab insights selection`() {
        startViewModel()

        viewModel.onSectionSelected(INSIGHTS)

        verify(statsSectionManager).setSelectedSection(INSIGHTS)
        /* First one is default insights section selection which is set when no value is passed to vm for
           initial section */
        verify(analyticsTracker, times(2)).track(STATS_INSIGHTS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab days selection`() {
        startViewModel()

        viewModel.onSectionSelected(DAYS)

        verify(statsSectionManager).setSelectedSection(DAYS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_DAYS_ACCESSED, StatsGranularity.DAYS)
    }

    @Test
    fun `stores and tracks tab weeks selection`() {
        startViewModel()

        viewModel.onSectionSelected(WEEKS)

        verify(statsSectionManager).setSelectedSection(WEEKS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_WEEKS_ACCESSED, StatsGranularity.WEEKS)
    }

    @Test
    fun `stores and tracks tab months selection`() {
        startViewModel()

        viewModel.onSectionSelected(MONTHS)

        verify(statsSectionManager).setSelectedSection(MONTHS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_MONTHS_ACCESSED, StatsGranularity.MONTHS)
    }

    @Test
    fun `stores and tracks tab years selection`() {
        startViewModel()

        viewModel.onSectionSelected(YEARS)

        verify(statsSectionManager).setSelectedSection(YEARS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_YEARS_ACCESSED, StatsGranularity.YEARS)
    }

    @Test
    fun `shows shadow on the insights tab`() {
        var toolbarHasShadow: Boolean? = null
        startViewModel()

        viewModel.toolbarHasShadow.observeForever { toolbarHasShadow = it }

        assertThat(toolbarHasShadow).isNull()

        _liveSelectedSection.value = INSIGHTS

        assertThat(toolbarHasShadow).isTrue

        _liveSelectedSection.value = DAYS

        assertThat(toolbarHasShadow).isFalse
    }

    @Test
    fun `propagates site change event to base list use case`() = test {
        startViewModel()

        viewModel.onSiteChanged()

        verify(baseListUseCase).refreshData(true)
    }
    @Test
    fun `given stats module enabled, when started, then state reflects enabled view`() = test {
        val uiModel = initObservers().uiModelObserver

        startViewModel(statsModuleEnabled = true)

        assertThat(uiModel.last().disabledStatsViewVisible).isFalse
    }

    @Test
    fun `given stats module disabled, when started, then state reflects disabled view`() = test {
        val uiModel = initObservers().uiModelObserver

        startViewModel(statsModuleEnabled = false)

        assertThat(uiModel.last().disabledStatsViewVisible).isTrue
    }

    @Test
    fun `given enable stats module is clicked, then state reflects progress`() = test {
        val uiModel = initObservers().uiModelObserver

        startViewModel(statsModuleEnabled = false)
        viewModel.onEnableStatsModuleClick()

        assertThat(uiModel.last().disabledStatsProgressVisible).isTrue
    }

    @Test
    fun `given enable stats module is clicked, when no network connection, then a snackbar message is shown`() = test {
        whenever(statsModuleActivateUseCase.postActivateStatsModule(anyOrNull())).thenReturn(networkUnavailableError)

        val msgs = initObservers().snackbarMessages

        startViewModel(statsModuleEnabled = false)
        viewModel.onEnableStatsModuleClick()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.no_network_title))
    }

    @Test
    fun `given enable stats module is clicked, when request fails, then a snackbar message is shown`() = test {
        whenever(statsModuleActivateUseCase.postActivateStatsModule(anyOrNull())).thenReturn(remoteRequestFailure)

        val msgs = initObservers().snackbarMessages

        startViewModel(statsModuleEnabled = false)
        viewModel.onEnableStatsModuleClick()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.stats_disabled_enable_stats_error_message))
    }

    @Test
    fun `given enable stats module is clicked, when request succeeds, then disabled view is hidden`() = test {
        whenever(statsModuleActivateUseCase.postActivateStatsModule(anyOrNull())).thenReturn(success)

        val uiModel = initObservers().uiModelObserver

        startViewModel(statsModuleEnabled = false)
        viewModel.onEnableStatsModuleClick()

        assertThat(uiModel.last().disabledStatsViewVisible).isFalse
    }

    @Ignore("Disabled until next sprint") @Test
    fun `given wp app, when jetpack powered bottom sheet feature is on, then jp powered bottom sheet is shown`() {
        val showJetpackPoweredBottomSheetEvent = mutableListOf<Event<Boolean>>()
        viewModel.showJetpackPoweredBottomSheet.observeForever {
            showJetpackPoweredBottomSheetEvent.add(it)
        }
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(true)

        startViewModel()

        assertThat(showJetpackPoweredBottomSheetEvent.last().peekContent()).isTrue
    }

    @Test
    fun `given wp app, when jetpack powered bottom sheet feature is off, then jp powered bottom sheet is not shown`() {
        val showJetpackPoweredBottomSheetEvent = mutableListOf<Event<Boolean>>(Event(false))
        viewModel.showJetpackPoweredBottomSheet.observeForever {
            showJetpackPoweredBottomSheetEvent.add(it)
        }
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(false)

        startViewModel()

        assertThat(showJetpackPoweredBottomSheetEvent.last().peekContent()).isFalse
    }

    @Test
    fun `given wp app, when jetpack overlay feature is false, then jp fullscreen overlay is not shown`() {
        val showJetpackOverlayEvent = mutableListOf<Event<Boolean>>(Event(false))
        viewModel.showJetpackOverlay.observeForever {
            showJetpackOverlayEvent.add(it)
        }
        whenever(jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(STATS)).thenReturn(false)

        startViewModel()

        assertThat(showJetpackOverlayEvent.last().peekContent()).isFalse
    }

    @Test
    fun `given wp app, when jetpack overlay feature is true, then jp fullscreen overlay is shown`() {
        val showJetpackOverlayEvent = mutableListOf<Event<Boolean>>(Event(false))
        viewModel.showJetpackOverlay.observeForever {
            showJetpackOverlayEvent.add(it)
        }
        whenever(jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(STATS)).thenReturn(true)

        startViewModel()

        assertThat(showJetpackOverlayEvent.last().peekContent()).isTrue()
    }

    private fun initObservers(): Observers {
        val uiModelChangeObserver = mutableListOf<StatsModuleUiModel>()
        viewModel.statsModuleUiModel.observeForever { uiModelChangeObserver.add(it.peekContent()) }

        val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
        viewModel.showSnackbarMessage.observeForever { snackbarMessages.add(it) }

        return Observers(uiModelChangeObserver, snackbarMessages)
    }

    private data class Observers(
        val uiModelObserver: List<StatsModuleUiModel>,
        val snackbarMessages: List<SnackbarMessageHolder>
    )

    private fun startViewModel(statsModuleEnabled: Boolean = true) {
        whenever(site.isActiveModuleEnabled(any())).thenReturn(statsModuleEnabled)
        viewModel.start(1, false, null, null, false, null)
    }

    private val networkUnavailableError = StatsModuleActivateRequestState.Failure.NetworkUnavailable
    private val remoteRequestFailure = StatsModuleActivateRequestState.Failure.RemoteRequestFailure
    private val success = StatsModuleActivateRequestState.Success
}
