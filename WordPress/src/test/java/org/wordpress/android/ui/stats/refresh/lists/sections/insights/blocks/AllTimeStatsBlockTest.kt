package org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks

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
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.Failed
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type
import org.wordpress.android.ui.stats.refresh.lists.ListInsightItem

class AllTimeStatsBlockTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: AllTimeStatsBlock
    @Before
    fun setUp() {
        useCase = AllTimeStatsBlock(
                Dispatchers.Unconfined,
                insightsStore
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
        ).thenReturn(OnInsightsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is Failed)
        assertEquals(result.type, Type.FAILED)
        assertEquals((result as Failed).errorMessage, message)
        assertEquals(result.failedType, R.string.stats_insights_all_time_stats)
    }

    @Test
    fun `result contains only empty item when response is empty`() = test {
        val forced = false
        val refresh = true
        val emptyModel = InsightsAllTimeModel(1L, null, 0, 0, 0, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(emptyModel))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
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
        val model = InsightsAllTimeModel(1L, null, 0, 0, posts, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Item)
        val item = items[1] as Item
        assertEquals(item.icon, R.drawable.ic_posts_grey_dark_24dp)
        assertEquals(item.textResource, R.string.posts)
        assertEquals(item.value, posts.toString())
    }

    @Test
    fun `result contains view item when views gt 0`() = test {
        val forced = false
        val refresh = true
        val views = 15
        val model = InsightsAllTimeModel(1L, null, 0, views, 0, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Item)
        val item = items[1] as Item
        assertEquals(item.icon, R.drawable.ic_visible_on_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_views)
        assertEquals(item.value, views.toString())
    }

    @Test
    fun `result contains visitors item when views gt 0`() = test {
        val forced = false
        val refresh = true
        val visitors = 20
        val model = InsightsAllTimeModel(1L, null, visitors, 0, 0, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Item)
        val item = items[1] as Item
        assertEquals(item.icon, R.drawable.ic_user_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_visitors)
        assertEquals(item.value, visitors.toString())
    }

    @Test
    fun `result contains best day total item when it is gt 0`() = test {
        val forced = false
        val refresh = true
        val bestDayTotal = 20
        val model = InsightsAllTimeModel(1L, null, 0, 0, 0, "MONDAY", bestDayTotal)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Item)
        val item = items[1] as Item
        assertEquals(item.icon, R.drawable.ic_trophy_grey_dark_24dp)
        assertEquals(item.textResource, R.string.stats_insights_best_ever)
        assertEquals(item.value, bestDayTotal.toString())
    }

    @Test
    fun `shows divider between items`() = test {
        val forced = false
        val refresh = true
        val model = InsightsAllTimeModel(1L, null, 10, 15, 0, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(model))

        val result = loadAllTimeInsights(refresh, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.BLOCK_LIST)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 3)
        assertTrue(items[1] is Item)
        assertTrue(items[2] is Item)
        assertEquals((items[1] as Item).showDivider, true)
        assertEquals((items[2] as Item).showDivider, false)
    }

    private suspend fun loadAllTimeInsights(refresh: Boolean, forced: Boolean): StatsListItem {
        var result: StatsListItem? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
