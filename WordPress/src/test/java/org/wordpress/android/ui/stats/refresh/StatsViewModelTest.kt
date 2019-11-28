package org.wordpress.android.ui.stats.refresh

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.test
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
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

class StatsViewModelTest : BaseUnitTest() {
    @Mock lateinit var baseListUseCase: BaseListUseCase
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var newsCardHandler: NewsCardHandler
    @Mock lateinit var site: SiteModel
    private lateinit var viewModel: StatsViewModel
    private val _liveSelectedSection = MutableLiveData<StatsSection>()
    private val liveSelectedSection: LiveData<StatsSection> = _liveSelectedSection
    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        whenever(baseListUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(statsSectionManager.liveSelectedSection).thenReturn(liveSelectedSection)
        viewModel = StatsViewModel(
                mapOf(DAYS to baseListUseCase),
                Dispatchers.Unconfined,
                selectedDateProvider,
                statsSectionManager,
                analyticsTracker,
                networkUtilsWrapper,
                statsSiteProvider,
                newsCardHandler
        )

        viewModel.start(1, false, null, null, false)
    }

    @Test
    fun `stores and tracks tab insights selection`() {
        viewModel.onSectionSelected(INSIGHTS)

        verify(statsSectionManager).setSelectedSection(INSIGHTS)
        verify(analyticsTracker).track(STATS_INSIGHTS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab days selection`() {
        viewModel.onSectionSelected(DAYS)

        verify(statsSectionManager).setSelectedSection(DAYS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_DAYS_ACCESSED, StatsGranularity.DAYS)
    }

    @Test
    fun `stores and tracks tab weeks selection`() {
        viewModel.onSectionSelected(WEEKS)

        verify(statsSectionManager).setSelectedSection(WEEKS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_WEEKS_ACCESSED, StatsGranularity.WEEKS)
    }

    @Test
    fun `stores and tracks tab months selection`() {
        viewModel.onSectionSelected(MONTHS)

        verify(statsSectionManager).setSelectedSection(MONTHS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_MONTHS_ACCESSED, StatsGranularity.MONTHS)
    }

    @Test
    fun `stores and tracks tab years selection`() {
        viewModel.onSectionSelected(YEARS)

        verify(statsSectionManager).setSelectedSection(YEARS)
        verify(analyticsTracker).trackGranular(STATS_PERIOD_YEARS_ACCESSED, StatsGranularity.YEARS)
    }

    @Test
    fun `shows shadow on the insights tab`() {
        var toolbarHasShadow: Boolean? = null

        viewModel.toolbarHasShadow.observeForever { toolbarHasShadow = it }

        assertThat(toolbarHasShadow).isNull()

        _liveSelectedSection.value = INSIGHTS

        assertThat(toolbarHasShadow).isTrue()

        _liveSelectedSection.value = DAYS

        assertThat(toolbarHasShadow).isFalse()
    }

    @Test
    fun `propagates site change event to base list use case`() = test {
        viewModel.onSiteChanged()

        verify(baseListUseCase).refreshData(true)
    }
}
