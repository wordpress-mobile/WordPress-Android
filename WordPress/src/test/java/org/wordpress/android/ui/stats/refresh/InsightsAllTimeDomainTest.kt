package org.wordpress.android.ui.stats.refresh

import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type

@RunWith(MockitoJUnitRunner::class)
class InsightsAllTimeDomainTest {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    private lateinit var insightsAllTimeDomain: InsightsAllTimeDomain
    @Before
    fun setUp() {
        insightsAllTimeDomain = InsightsAllTimeDomain(insightsStore)
    }

    @Test
    fun `returns failed item when store fails`() = test {
        val forced = false
        val message = "error"
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(StatsError(GENERIC_ERROR, message)))

        val result = insightsAllTimeDomain.loadAllTimeInsights(site, forced)

        assertTrue(result is Failed)
        assertEquals(result.type, Type.FAILED)
        assertEquals((result as Failed).errorMessage, message)
        assertEquals(result.failedType, R.string.stats_insights_all_time)
    }

    @Test
    fun `item contains only empty item when response is empty`() = test {
        val forced = false
        val emptyModel = InsightsAllTimeModel(1L, null, 0, 0, 0, "MONDAY", 0)
        whenever(
                insightsStore.fetchAllTimeInsights(
                        site,
                        forced
                )
        ).thenReturn(OnInsightsFetched(emptyModel))

        val result = insightsAllTimeDomain.loadAllTimeInsights(site, forced)

        assertTrue(result is ListInsightItem)
        assertEquals(result.type, Type.LIST_INSIGHTS)
        val items = (result as ListInsightItem).items
        assertEquals(items.size, 2)
        assertTrue(items[0] is Title)
        assertEquals((items[0] as Title).text, R.string.stats_insights_all_time_stats)
        assertTrue(items[1] is Empty)
    }
}
