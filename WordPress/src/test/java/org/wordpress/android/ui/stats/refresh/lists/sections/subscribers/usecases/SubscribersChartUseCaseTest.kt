package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.subscribers.SubscribersStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class SubscribersChartUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var store: SubscribersStore

    @Mock
    lateinit var statsDateFormatter: StatsDateFormatter

    @Mock
    lateinit var subscribersMapper: SubscribersMapper

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var subscribersChartItemChartItem: SubscribersChartItem

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    private lateinit var useCase: SubscribersChartUseCase
    private val site = SiteModel()
    private val siteId = 1L
    private val periodData = PeriodData("2024-04-24", 10)
    private val modelPeriod = "2024-04-24"
    private val limitMode = Top(30)
    private val statsGranularity = DAYS
    private val model = SubscribersModel(modelPeriod, listOf(periodData))

    @Before
    fun setUp() {
        useCase = SubscribersChartUseCase(
            store,
            statsSiteProvider,
            subscribersMapper,
            testDispatcher(),
            testDispatcher(),
            analyticsTrackerWrapper
        )
        site.siteId = siteId
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(subscribersMapper.buildChart(any(), any())).thenReturn(subscribersChartItemChartItem)
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        whenever(store.getSubscribers(site, statsGranularity, limitMode)).thenReturn(model)
        whenever(store.fetchSubscribers(site, statsGranularity, limitMode, forced)).thenReturn(OnStatsFetched(model))

        val result = loadData(true, forced)

        Assertions.assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply { Assertions.assertThat(this[1]).isEqualTo(subscribersChartItemChartItem) }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchSubscribers(site, statsGranularity, limitMode, forced))
            .thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadData(true, forced)

        Assertions.assertThat(result.state).isEqualTo(ERROR)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
