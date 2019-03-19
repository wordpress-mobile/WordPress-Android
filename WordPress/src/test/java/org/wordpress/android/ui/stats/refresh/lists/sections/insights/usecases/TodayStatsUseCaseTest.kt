package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider

class TodayStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: TodayStatsUseCase
    private val views = 10
    private val visitors = 15
    private val likes = 20
    private val comments = 30
    @Before
    fun setUp() {
        useCase = TodayStatsUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsSiteProvider
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
    }

    @Test
    fun `maps full stats item to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchTodayInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        VisitsModel("2018-10-02", views, visitors, likes, 0, comments, 0)
                )
        )

        val result = loadTodayStats(refresh, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.TODAY_STATS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(5)
            assertTitle(this[0])
            assertViews(this[1], showDivider = true)
            assertVisitors(this[2], showDivider = true)
            assertLikes(this[3], showDivider = true)
            assertComments(this[4], showDivider = false)
        }
    }

    @Test
    fun `maps partial stats item to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchTodayInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        VisitsModel("2018-10-02", 0, visitors, likes, 0, 0, 0)
                )
        )

        val result = loadTodayStats(refresh, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.TODAY_STATS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertTitle(this[0])
            assertVisitors(this[1], showDivider = true)
            assertLikes(this[2], showDivider = false)
        }
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

        assertThat(result.type).isEqualTo(InsightsTypes.TODAY_STATS)
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

    private fun assertViews(blockListItem: BlockListItem, showDivider: Boolean = false) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        val item = blockListItem as ListItemWithIcon
        assertThat(item.textResource).isEqualTo(R.string.stats_views)
        assertThat(item.showDivider).isEqualTo(showDivider)
        assertThat(item.icon).isEqualTo(R.drawable.ic_visible_on_white_24dp)
        assertThat(item.value).isEqualTo(views.toString())
    }

    private fun assertVisitors(blockListItem: BlockListItem, showDivider: Boolean = false) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        val item = blockListItem as ListItemWithIcon
        assertThat(item.textResource).isEqualTo(R.string.stats_visitors)
        assertThat(item.showDivider).isEqualTo(showDivider)
        assertThat(item.icon).isEqualTo(R.drawable.ic_user_white_24dp)
        assertThat(item.value).isEqualTo(visitors.toString())
    }

    private fun assertLikes(blockListItem: BlockListItem, showDivider: Boolean = false) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        val item = blockListItem as ListItemWithIcon
        assertThat(item.textResource).isEqualTo(R.string.stats_likes)
        assertThat(item.showDivider).isEqualTo(showDivider)
        assertThat(item.icon).isEqualTo(R.drawable.ic_star_white_24dp)
        assertThat(item.value).isEqualTo(likes.toString())
    }

    private fun assertComments(blockListItem: BlockListItem, showDivider: Boolean = false) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        val item = blockListItem as ListItemWithIcon
        assertThat(item.textResource).isEqualTo(R.string.stats_comments)
        assertThat(item.showDivider).isEqualTo(showDivider)
        assertThat(item.icon).isEqualTo(R.drawable.ic_comment_white_24dp)
        assertThat(item.value).isEqualTo(comments.toString())
    }

    private suspend fun loadTodayStats(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
