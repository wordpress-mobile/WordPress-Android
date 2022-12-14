package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
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
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils

@ExperimentalCoroutinesApi
class TodayStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: TodayInsightsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock lateinit var statsWidgetUpdaters: StatsWidgetUpdaters
    @Mock lateinit var statsUtils: StatsUtils
    private lateinit var useCase: TodayStatsUseCase
    private val views = 10
    private val visitors = 15
    private val likes = 20
    private val comments = 30
    private val site = SiteModel()
    private val siteId = 1L

    @Before
    fun setUp() {
        useCase = TodayStatsUseCase(
                Dispatchers.Unconfined,
                testDispatcher(),
                insightsStore,
                statsSiteProvider,
                statsWidgetUpdaters,
                statsUtils,
                popupMenuHandler
        )
        site.siteId = siteId
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps stats item to UI model`() = test {
        val forced = false
        val refresh = true
        val model = VisitsModel("2018-10-02", views, visitors, likes, 0, comments, 0)
        whenever(insightsStore.getTodayInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchTodayInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadTodayStats(refresh, forced)

        assertThat(result.type).isEqualTo(InsightType.TODAY_STATS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertTitle(this[0])
            assertViewsAndVisitors(this[1])
            assertLikesAndComments(this[2])
        }
        verify(statsWidgetUpdaters, times(2)).updateTodayWidget(siteId)
    }

    @Test
    fun `maps empty stats item to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchTodayInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        VisitsModel("2018-10-02", 0, 0, 0, 0, 0, 0)
                )
        )

        val result = loadTodayStats(refresh, forced)

        assertThat(result.type).isEqualTo(InsightType.TODAY_STATS)
        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        result.stateData!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchTodayInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadTodayStats(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_insights_today_stats)
    }

    private fun assertViewsAndVisitors(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(QUICK_SCAN_ITEM)
        val item = blockListItem as QuickScanItem
        assertThat(item.startColumn.label).isEqualTo(R.string.stats_views)
        assertThat(item.startColumn.value).isEqualTo(views.toString())
        assertThat(item.endColumn.label).isEqualTo(R.string.stats_visitors)
        assertThat(item.endColumn.value).isEqualTo(visitors.toString())
    }

    private fun assertLikesAndComments(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(QUICK_SCAN_ITEM)
        val item = blockListItem as QuickScanItem
        assertThat(item.startColumn.label).isEqualTo(R.string.stats_likes)
        assertThat(item.startColumn.value).isEqualTo(likes.toString())
        assertThat(item.endColumn.label).isEqualTo(R.string.stats_comments)
        assertThat(item.endColumn.value).isEqualTo(comments.toString())
    }

    private suspend fun loadTodayStats(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
