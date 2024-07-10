package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.subscribers.SubscribersStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE_WITH_MORE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_WITH_CHART_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TotalFollowersUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var subscribersStore: SubscribersStore

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var totalStatsMapper: TotalStatsMapper

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var useCaseMode: UseCaseMode

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var actionCardHandler: ActionCardHandler
    private lateinit var useCase: TotalFollowersUseCase
    private val subscribers = 10L

    @Before
    fun setUp() {
        useCase = TotalFollowersUseCase(
            testDispatcher(),
            testDispatcher(),
            subscribersStore,
            statsSiteProvider,
            resourceProvider,
            totalStatsMapper,
            analyticsTrackerWrapper,
            actionCardHandler,
            useCaseMode,
            statsUtils
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps summary to UI model`() = test {
        val forced = false
        val refresh = true
        val periodData = SubscribersModel.PeriodData("2024-04-24", subscribers)
        val modelPeriod = "2024-05-03"
        val model = SubscribersModel(modelPeriod, listOf(periodData))
        whenever(subscribersStore.getSubscribers(site, StatsGranularity.DAYS, LimitMode.Top(1))).thenReturn(model)
        whenever(subscribersStore.fetchSubscribers(site, StatsGranularity.DAYS, LimitMode.Top(1)))
            .thenReturn(OnStatsFetched(model))

        val result = loadSummary(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
            assertValue(this[1])
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(subscribersStore.fetchSubscribers(site, StatsGranularity.DAYS, LimitMode.Top(1), forced))
            .thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadSummary(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE_WITH_MORE)
        assertThat((item as TitleWithMore).textResource).isEqualTo(R.string.stats_view_total_subscribers)
    }

    private fun assertValue(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(VALUE_WITH_CHART_ITEM)
        val item = blockListItem as ValueWithChartItem
        assertThat(item.value).isEqualTo(subscribers.toString())
    }

    private suspend fun loadSummary(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
