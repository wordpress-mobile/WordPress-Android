package org.wordpress.android.fluxc.persistance.stats

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import org.wordpress.android.fluxc.store.stats.ALL_TIME_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class InsightsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var statsRequestSqlUtils: StatsRequestSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var insightsSqlUtils: TestSqlUtils

    @Before
    fun setUp() {
        insightsSqlUtils = TestSqlUtils(statsSqlUtils, statsRequestSqlUtils)
    }

    @Test
    fun `returns response from stats utils`() {
        whenever(statsSqlUtils.select(site, ALL_TIME_INSIGHTS, INSIGHTS, AllTimeResponse::class.java)).thenReturn(
                ALL_TIME_RESPONSE
        )

        val result = insightsSqlUtils.select(site)

        assertEquals(result, ALL_TIME_RESPONSE)
    }

    @Test
    fun `inserts response to stats utils`() {
        insightsSqlUtils.insert(site, ALL_TIME_RESPONSE)

        verify(statsSqlUtils).insert(site, ALL_TIME_INSIGHTS, INSIGHTS, ALL_TIME_RESPONSE, true)
    }

    class TestSqlUtils(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<AllTimeResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            BlockType.ALL_TIME_INSIGHTS,
            AllTimeResponse::class.java
    )
}
