package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

class StatsViewModelTest : BaseUnitTest() {
    @Mock lateinit var insightsUseCase: BaseListUseCase
    @Mock lateinit var dayStatsUseCase: BaseListUseCase
    @Mock lateinit var weekStatsUseCase: BaseListUseCase
    @Mock lateinit var monthStatsUseCase: BaseListUseCase
    @Mock lateinit var yearStatsUseCase: BaseListUseCase
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var site: SiteModel
    private lateinit var viewModel: StatsViewModel
    private val selectedDate = Date(0)
    private val selectedDateLabel = "Jan 1"
    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        whenever(insightsUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(dayStatsUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(weekStatsUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(monthStatsUseCase.snackbarMessage).thenReturn(MutableLiveData())
        whenever(yearStatsUseCase.snackbarMessage).thenReturn(MutableLiveData())
        viewModel = StatsViewModel(
                insightsUseCase,
                dayStatsUseCase,
                weekStatsUseCase,
                monthStatsUseCase,
                yearStatsUseCase,
                Dispatchers.Unconfined,
                selectedDateProvider,
                statsDateFormatter,
                statsSectionManager,
                analyticsTracker,
                networkUtilsWrapper
        )

        whenever(selectedDateProvider.getSelectedDate(any())).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(eq(selectedDate), any())).thenReturn(selectedDateLabel)
        whenever(statsSectionManager.getSelectedSection()).thenReturn(INSIGHTS)
        viewModel.start(site, false, null)
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
        verify(analyticsTracker).track(STATS_PERIOD_DAYS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab weeks selection`() {
        viewModel.onSectionSelected(WEEKS)

        verify(statsSectionManager).setSelectedSection(WEEKS)
        verify(analyticsTracker).track(STATS_PERIOD_WEEKS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab months selection`() {
        viewModel.onSectionSelected(MONTHS)

        verify(statsSectionManager).setSelectedSection(MONTHS)
        verify(analyticsTracker).track(STATS_PERIOD_MONTHS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab years selection`() {
        viewModel.onSectionSelected(YEARS)

        verify(statsSectionManager).setSelectedSection(YEARS)
        verify(analyticsTracker).track(STATS_PERIOD_YEARS_ACCESSED)
    }

    @Test
    fun `hides date selector on insights screen`() {
        var model: DateSelectorUiModel? = null
        viewModel.showDateSelector.observeForever { model = it }

        viewModel.onSectionSelected(INSIGHTS)

        assertThat(model).isNotNull
        assertThat(model?.isVisible).isFalse()
    }

    @Test
    fun `does not reemit hidden date selector`() {
        val models = mutableListOf<DateSelectorUiModel>()
        viewModel.showDateSelector.observeForever { model -> model?.let { models.add(it) } }

        viewModel.onSectionSelected(INSIGHTS)

        assertThat(models).hasSize(1)

        viewModel.onSectionSelected(INSIGHTS)

        assertThat(models).hasSize(1)
    }

    @Test
    fun `shows date selector on days screen`() {
        val statsGranularity = StatsGranularity.DAYS
        val selectedDate = Date(0)
        val label = "Jan 1"
        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(label)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsGranularity)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        viewModel.showDateSelector.observeForever { model = it }

        viewModel.onSectionSelected(DAYS)

        assertThat(model).isNotNull
        assertThat(model?.isVisible).isTrue()
        assertThat(model?.enableSelectPrevious).isTrue()
        assertThat(model?.enableSelectNext).isTrue()
        assertThat(model?.date).isEqualTo(label)
    }

    @Test
    fun `updates date selector on date change`() {
        val statsGranularity = StatsGranularity.DAYS
        val updatedDate = Date(10)
        val updatedLabel = "Jan 2"
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsGranularity)).thenReturn(true)
        whenever(statsSectionManager.getSelectedSection()).thenReturn(DAYS)
        var model: DateSelectorUiModel? = null
        viewModel.showDateSelector.observeForever { model = it }

        viewModel.onSectionSelected(DAYS)

        assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(updatedDate)

        viewModel.onSelectedDateChange(statsGranularity)

        assertThat(model?.date).isEqualTo(updatedLabel)
    }
}
