package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter

class AllTimeStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: AllTimeStatsUseCase
    private val bestDay = "2018-11-25"
    private val bestDayTransformed = "Nov 25, 2018"
    @Before
    fun setUp() {
        useCase = AllTimeStatsUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsDateFormatter
        )
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

        assertTrue(result is Error)
        assertEquals(result.type, Type.ERROR)
        assertEquals((result as Error).errorMessage, message)
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

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Empty)
    }

    @Test
    fun `result contains post item when posts gt 0`() = test {
        val forced = false
        val refresh = true
        val posts = 10
        val model = InsightsAllTimeModel(1L, null, 0, 0, posts, bestDay, 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is ListItemWithIcon)
        val item = items[1] as ListItemWithIcon
        assertEquals(item.icon, R.drawable.ic_posts_grey_dark_24dp)
        assertEquals(item.textResource, R.string.posts)
        assertEquals(item.value, posts.toString())
    }

    @Test
    fun `result contains view item when views gt 0`() = test {
        val forced = false
        val refresh = true
        val views = 15
        val model = InsightsAllTimeModel(1L, null, 0, views, 0, bestDay, 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is ListItemWithIcon)
        val item = items[1] as ListItemWithIcon
        assertEquals(item.icon, R.drawable.ic_visible_on_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_views)
        assertEquals(item.value, views.toString())
    }

    @Test
    fun `result contains visitors item when views gt 0`() = test {
        val forced = false
        val refresh = true
        val visitors = 20
        val model = InsightsAllTimeModel(1L, null, visitors, 0, 0, bestDay, 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is ListItemWithIcon)
        val item = items[1] as ListItemWithIcon
        assertEquals(item.icon, R.drawable.ic_user_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_visitors)
        assertEquals(item.value, visitors.toString())
    }

    @Test
    fun `result contains best day total item when it is gt 0`() = test {
        val forced = false
        val refresh = true
        val bestDayTotal = 20
        val model = InsightsAllTimeModel(1L, null, 0, 0, 0, bestDay, bestDayTotal)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))
        whenever(statsDateFormatter.parseDate(bestDay)).thenReturn(bestDayTransformed)

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is ListItemWithIcon)
        val item = items[1] as ListItemWithIcon
        assertEquals(item.icon, R.drawable.ic_trophy_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_insights_best_ever)
        assertEquals(item.value, bestDayTotal.toString())
        assertEquals(item.subText, bestDayTransformed)
    }

    @Test
    fun `shows divider between items`() = test {
        val forced = false
        val refresh = true
        val model = InsightsAllTimeModel(1L, null, 10, 15, 0, bestDay, 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is BlockList)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as BlockList).items
        assertEquals(items.size, 3)
        assertTrue(items[1] is ListItemWithIcon)
        assertTrue(items[2] is ListItemWithIcon)
        assertEquals((items[1] as ListItemWithIcon).showDivider, true)
        assertEquals((items[2] as ListItemWithIcon).showDivider, false)
    }

    private suspend fun loadAllTimeInsights(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
