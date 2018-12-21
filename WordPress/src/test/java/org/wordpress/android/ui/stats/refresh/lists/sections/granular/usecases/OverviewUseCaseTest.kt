package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

class OverviewUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: VisitsAndViewsStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var overviewMapper: OverviewMapper
    @Mock lateinit var site: SiteModel
    @Mock lateinit var columns: Columns
    @Mock lateinit var title: Title
    @Mock lateinit var barChartItem: BarChartItem
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var useCase: OverviewUseCase
    private val periodData = PeriodData("2018-10-08", 10, 15, 20, 25, 30, 35)
    private val modelPeriod = "2018-10-10"
    private val pageSize = 15
    private val statsGranularity = DAYS
    private val model = VisitsAndViewsModel(modelPeriod, listOf(periodData))
    private val currentDate = Date(10)
    @Before
    fun setUp() {
        useCase = OverviewUseCase(
                statsGranularity,
                store,
                selectedDateProvider,
                statsDateFormatter,
                overviewMapper,
                Dispatchers.Unconfined,
                analyticsTrackerWrapper
        )
        whenever(selectedDateProvider.getCurrentDate()).thenReturn(currentDate)
        whenever(overviewMapper.buildTitle(any(), any(), any(), any())).thenReturn(title)
        whenever(overviewMapper.buildChart(any(), any(), any(), any(), any())).thenReturn(barChartItem)
        whenever(overviewMapper.buildColumns(any(), any(), any())).thenReturn(columns)
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false

        whenever(store.fetchVisits(site, pageSize, currentDate, statsGranularity, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items[0]).isEqualTo(title)
            assertThat(this.items[1]).isEqualTo(barChartItem)
            assertThat(this.items[2]).isEqualTo(columns)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchVisits(site, pageSize, currentDate, statsGranularity, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            Assertions.assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
