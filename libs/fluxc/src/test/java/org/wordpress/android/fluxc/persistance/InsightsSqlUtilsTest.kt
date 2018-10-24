package org.wordpress.android.fluxc.persistance

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_STATS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.MOST_POPULAR_INSIGHTS
import org.wordpress.android.fluxc.store.ALL_TIME_RESPONSE
import org.wordpress.android.fluxc.store.LATEST_POST
import org.wordpress.android.fluxc.store.MOST_POPULAR_RESPONSE
import org.wordpress.android.fluxc.store.POST_STATS_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class InsightsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var insightsSqlUtils: InsightsSqlUtils

    @Before
    fun setUp() {
        insightsSqlUtils = InsightsSqlUtils(statsSqlUtils)
    }

    @Test
    fun `returns all time response from stats utils`() {
        whenever(statsSqlUtils.select(site, ALL_TIME_INSIGHTS, AllTimeResponse::class.java)).thenReturn(
                ALL_TIME_RESPONSE
        )

        val result = insightsSqlUtils.selectAllTimeInsights(site)

        assertEquals(result, ALL_TIME_RESPONSE)
    }

    @Test
    fun `inserts all time response to stats utils`() {
        insightsSqlUtils.insert(site, ALL_TIME_RESPONSE)

        verify(statsSqlUtils).insert(site, ALL_TIME_INSIGHTS, ALL_TIME_RESPONSE)
    }

    @Test
    fun `returns most popular response from stats utils`() {
        whenever(statsSqlUtils.select(site, MOST_POPULAR_INSIGHTS, MostPopularResponse::class.java)).thenReturn(
                MOST_POPULAR_RESPONSE
        )

        val result = insightsSqlUtils.selectMostPopularInsights(site)

        assertEquals(result, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `inserts most popular response to stats utils`() {
        insightsSqlUtils.insert(site, MOST_POPULAR_RESPONSE)

        verify(statsSqlUtils).insert(site, MOST_POPULAR_INSIGHTS, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns latest post detail response from stats utils`() {
        whenever(statsSqlUtils.select(site, LATEST_POST_DETAIL_INSIGHTS, PostResponse::class.java)).thenReturn(
                LATEST_POST
        )

        val result = insightsSqlUtils.selectLatestPostDetail(site)

        assertEquals(result, LATEST_POST)
    }

    @Test
    fun `inserts latest post detail response to stats utils`() {
        insightsSqlUtils.insert(site, LATEST_POST)

        verify(statsSqlUtils).insert(site, LATEST_POST_DETAIL_INSIGHTS, LATEST_POST)
    }

    @Test
    fun `returns latest post views response from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        LATEST_POST_STATS_INSIGHTS,
                        PostStatsResponse::class.java
                )
        ).thenReturn(
                POST_STATS_RESPONSE
        )

        val result = insightsSqlUtils.selectLatestPostStats(site)

        assertEquals(result, POST_STATS_RESPONSE)
    }

    @Test
    fun `inserts latest post views response to stats utils`() {
        insightsSqlUtils.insert(site, POST_STATS_RESPONSE)

        verify(statsSqlUtils).insert(site, LATEST_POST_STATS_INSIGHTS, POST_STATS_RESPONSE)
    }
}
