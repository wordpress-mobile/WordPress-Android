package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

class OverviewUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: VisitsAndViewsStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var overviewMapper: OverviewMapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var columns: Columns
    @Mock lateinit var title: ValueItem
    @Mock lateinit var barChartItem: BarChartItem
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var useCase: OverviewUseCase
    private val periodData = PeriodData("2018-10-08", 10, 15, 20, 25, 30, 35)
    private val modelPeriod = "2018-10-10"
    private val limitMode = Top(15)
    private val statsGranularity = DAYS
    private val model = VisitsAndViewsModel(modelPeriod, listOf(periodData))
    private val currentDate = Date(10)
    @Before
    fun setUp() {
        whenever(selectedDateProvider.granularSelectedDateChanged(statsGranularity)).thenReturn(MutableLiveData())
        useCase = OverviewUseCase(
                statsGranularity,
                store,
                selectedDateProvider,
                statsSiteProvider,
                statsDateFormatter,
                overviewMapper,
                Dispatchers.Unconfined,
                analyticsTrackerWrapper
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(selectedDateProvider.getCurrentDate()).thenReturn(currentDate)
        whenever(overviewMapper.buildTitle(any(), isNull(), any())).thenReturn(title)
        whenever(overviewMapper.buildChart(any(), any(), any(), any(), any(), any())).thenReturn(listOf(barChartItem))
        whenever(overviewMapper.buildColumns(any(), any(), any())).thenReturn(columns)
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        whenever(store.getVisits(site, statsGranularity, LimitMode.All, currentDate)).thenReturn(model)
        whenever(store.fetchVisits(site, statsGranularity, limitMode, currentDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsTypes.OVERVIEW)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this[0]).isEqualTo(title)
            assertThat(this[1]).isEqualTo(barChartItem)
            assertThat(this[2]).isEqualTo(columns)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchVisits(site, statsGranularity, limitMode, currentDate, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
