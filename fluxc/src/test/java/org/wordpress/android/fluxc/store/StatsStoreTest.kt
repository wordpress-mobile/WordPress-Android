package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightTypesModel
import org.wordpress.android.fluxc.persistence.InsightTypesSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class StatsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var insightTypesSqlUtils: InsightTypesSqlUtils
    private lateinit var store: StatsStore

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        store = StatsStore(
                Unconfined,
                insightTypesSqlUtils
        )
    }

    @Test
    fun `returns default stats types when DB is empty`() = test {
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(listOf())

        val result = store.getInsights(site)

        assertThat(result).containsExactly(LATEST_POST_SUMMARY, TODAY_STATS, ALL_TIME_STATS, POSTING_ACTIVITY)
    }

    @Test
    fun `returns updated stats types from DB`() = test {
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(listOf(COMMENTS))

        val result = store.getInsights(site)

        assertThat(result).containsExactly(COMMENTS)
    }

    @Test
    fun `updates types with added and removed`() = test {
        val addedTypes = listOf(
                COMMENTS
        )
        val removedTypes = listOf(
                LATEST_POST_SUMMARY
        )
        store.updateTypes(site, InsightTypesModel(addedTypes, removedTypes))

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(site, addedTypes)
        verify(insightTypesSqlUtils).insertOrReplaceRemovedItems(site, removedTypes)
    }

    @Test
    fun `moves type up in the list when it is last`() = test {
        val insightsTypes = listOf(
                LATEST_POST_SUMMARY,
                FOLLOWERS,
                COMMENTS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightsTypes)

        store.moveTypeUp(site, COMMENTS)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(site, listOf(LATEST_POST_SUMMARY, COMMENTS, FOLLOWERS))
    }

    @Test
    fun `does not move type up in the list when it is first`() = test {
        val insightsTypes = listOf(
                COMMENTS,
                LATEST_POST_SUMMARY,
                FOLLOWERS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightsTypes)

        store.moveTypeUp(site, COMMENTS)

        verify(insightTypesSqlUtils, never()).insertOrReplaceAddedItems(eq(site), any())
    }

    @Test
    fun `moves type down in the list when it is first`() = test {
        val insightsTypes = listOf(
                LATEST_POST_SUMMARY,
                FOLLOWERS,
                COMMENTS
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightsTypes)

        store.moveTypeDown(site, LATEST_POST_SUMMARY)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(site, listOf(FOLLOWERS, LATEST_POST_SUMMARY, COMMENTS))
    }

    @Test
    fun `does not move type down in the list when it is last`() = test {
        val insightsTypes = listOf(
                COMMENTS,
                FOLLOWERS,
                LATEST_POST_SUMMARY
        )
        whenever(insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)).thenReturn(insightsTypes)

        store.moveTypeDown(site, LATEST_POST_SUMMARY)

        verify(insightTypesSqlUtils, never()).insertOrReplaceAddedItems(eq(site), any())
    }

    @Test
    fun `removes type from list`() = test {
        store.removeType(site, LATEST_POST_SUMMARY)

        verify(insightTypesSqlUtils).insertOrReplaceAddedItems(
                site,
                listOf(TODAY_STATS, ALL_TIME_STATS, POSTING_ACTIVITY)
        )

        verify(insightTypesSqlUtils).insertOrReplaceRemovedItems(
                site,
                listOf(
                        MOST_POPULAR_DAY_AND_HOUR,
                        FOLLOWER_TOTALS,
                        TAGS_AND_CATEGORIES,
                        ANNUAL_SITE_STATS,
                        COMMENTS,
                        FOLLOWERS,
                        PUBLICIZE,
                        LATEST_POST_SUMMARY
                )
        )
    }
}
