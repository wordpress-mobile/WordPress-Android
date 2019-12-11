package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

class OverviewUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: VisitsAndViewsStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var overviewMapper: OverviewMapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var columns: Columns
    @Mock lateinit var title: ValueItem
    @Mock lateinit var barChartItem: BarChartItem
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var statsWidgetUpdaters: StatsWidgetUpdaters
    private lateinit var useCase: OverviewUseCase
    private val site = SiteModel()
    private val siteId = 1L
    private val periodData = PeriodData("2018-10-08", 10, 15, 20, 25, 30, 35)
    private val modelPeriod = "2018-10-10"
    private val limitMode = Top(15)
    private val statsGranularity = DAYS
    private val model = VisitsAndViewsModel(modelPeriod, listOf(periodData))
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = OverviewUseCase(
                statsGranularity,
                store,
                selectedDateProvider,
                statsSiteProvider,
                statsDateFormatter,
                overviewMapper,
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
                statsWidgetUpdaters,
                resourceProvider
        )
        site.siteId = siteId
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(overviewMapper.buildTitle(any(), isNull(), any(), any(), any(), any())).thenReturn(title)
        whenever(overviewMapper.buildChart(any(), any(), any(), any(), any(), any())).thenReturn(listOf(barChartItem))
        whenever(overviewMapper.buildColumns(any(), any(), any())).thenReturn(columns)
        whenever(resourceProvider.getString(R.string.stats_loading_card)).thenReturn("Loading")
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        whenever(store.getVisits(site, statsGranularity, LimitMode.All)).thenReturn(model)
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.OVERVIEW)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this[0]).isEqualTo(title)
            assertThat(this[1]).isEqualTo(barChartItem)
            assertThat(this[2]).isEqualTo(columns)
        }
        verify(statsWidgetUpdaters, times(2)).updateViewsWidget(siteId)
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
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
