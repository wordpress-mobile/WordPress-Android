package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils

@ExperimentalCoroutinesApi
class AllTimeStatsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var insightsStore: AllTimeInsightsStore
    @Mock
    lateinit var statsDateFormatter: StatsDateFormatter
    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider
    @Mock
    lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock
    lateinit var statsWidgetUpdaters: StatsWidgetUpdaters
    @Mock
    lateinit var statsUtils: StatsUtils
    private val site = SiteModel()
    private lateinit var useCase: AllTimeStatsUseCase
    private val bestDay = "2018-11-25"
    private val bestDayTransformed = "Nov 25, 2018"
    private val siteId = 1L

    @Before
    fun setUp() {
        useCase = AllTimeStatsUseCase(
            testDispatcher(),
            testDispatcher(),
            insightsStore,
            statsSiteProvider,
            statsDateFormatter,
            statsWidgetUpdaters,
            statsUtils,
            popupMenuHandler
        )
        site.siteId = siteId
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsDateFormatter.printDate(bestDay)).thenReturn(bestDayTransformed)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `returns failed item when store fails`() = test {
        val forced = false
        val refresh = true
        val message = "error"
        whenever(
            insightsStore.fetchAllTimeInsights(
                site,
                forced
            )
        ).thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadAllTimeInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
        assertThat(result.type).isEqualTo(InsightType.ALL_TIME_STATS)
    }

    @Test
    fun `result contains only empty item when response is empty`() = test {
        val forced = false
        val refresh = true
        val emptyModel = InsightsAllTimeModel(1L, null, 0, 0, 0, bestDay, 0)
        whenever(
            insightsStore.fetchAllTimeInsights(
                site,
                forced
            )
        ).thenReturn(OnStatsFetched(emptyModel))

        val result = loadAllTimeInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        assertThat(result.type).isEqualTo(InsightType.ALL_TIME_STATS)
        val items = result.stateData!!
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).textResource, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Empty)
    }

    @Test
    fun `result contains post item when posts gt 0`() = test {
        val forced = false
        val refresh = true
        val posts = 10
        val visitors = 15
        val views = 0
        val viewsBestDayTotal = 100
        val model = InsightsAllTimeModel(1L, null, visitors, views, posts, bestDay, viewsBestDayTotal)
        whenever(insightsStore.getAllTimeInsights(site)).thenReturn(model)
        whenever(
            insightsStore.fetchAllTimeInsights(
                site,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(InsightType.ALL_TIME_STATS)
        val items = result.data!!
        assertEquals(items.size, 3)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).textResource, R.string.stats_insights_all_time_stats)
        (items[1] as QuickScanItem).apply {
            assertThat(this.startColumn.label).isEqualTo(R.string.stats_views)
            assertThat(this.startColumn.value).isEqualTo(views.toString())
            assertThat(this.endColumn.label).isEqualTo(R.string.stats_visitors)
            assertThat(this.endColumn.value).isEqualTo(visitors.toString())
        }
        (items[2] as QuickScanItem).apply {
            assertThat(this.startColumn.label).isEqualTo(R.string.posts)
            assertThat(this.startColumn.value).isEqualTo(posts.toString())
            assertThat(this.endColumn.label).isEqualTo(R.string.stats_insights_best_ever)
            assertThat(this.endColumn.value).isEqualTo(viewsBestDayTotal.toString())
            assertThat(this.endColumn.highest).isEqualTo(bestDayTransformed)
        }
        verify(statsWidgetUpdaters, times(2)).updateAllTimeWidget(siteId)
    }

    @Test
    fun `best day is null when it's empty`() = test {
        val forced = false
        val refresh = true
        val model = InsightsAllTimeModel(1L, null, 1, 0, 0, "", 0)
        whenever(insightsStore.getAllTimeInsights(site)).thenReturn(model)
        whenever(
            insightsStore.fetchAllTimeInsights(
                site,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(InsightType.ALL_TIME_STATS)
        val items = result.data!!
        assertEquals(items.size, 3)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).textResource, R.string.stats_insights_all_time_stats)
        (items[2] as QuickScanItem).apply {
            assertThat(this.endColumn.tooltip).isNull()
        }
    }

    private suspend fun loadAllTimeInsights(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
