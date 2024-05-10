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
import org.wordpress.android.fluxc.model.stats.SummaryModel
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.SummaryStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.VALUE_WITH_CHART_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases.TotalSubscribersUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils

@ExperimentalCoroutinesApi
class TotalSubscribersUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var insightsStore: SummaryStore

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var statsUtils: StatsUtils

    private lateinit var useCase: TotalSubscribersUseCase
    private val followers = 100

    @Before
    fun setUp() {
        useCase = TotalSubscribersUseCase(
            testDispatcher(),
            testDispatcher(),
            insightsStore,
            statsSiteProvider,
            statsUtils
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps summary to UI model`() = test {
        val forced = false
        val refresh = true
        val model = SummaryModel(0, 0, followers)
        whenever(insightsStore.getSummary(site)).thenReturn(model)
        whenever(insightsStore.fetchSummary(site, forced)).thenReturn(OnStatsFetched(model))

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
        whenever(insightsStore.fetchSummary(site, forced)).thenReturn(
            OnStatsFetched(StatsError(GENERIC_ERROR, message))
        )

        val result = loadSummary(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_total_subscribers)
    }

    private fun assertValue(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(VALUE_WITH_CHART_ITEM)
        val item = blockListItem as ValueWithChartItem
        assertThat(item.value).isEqualTo(followers.toString())
    }

    private suspend fun loadSummary(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
