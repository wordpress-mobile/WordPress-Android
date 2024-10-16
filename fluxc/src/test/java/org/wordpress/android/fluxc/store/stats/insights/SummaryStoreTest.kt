package org.wordpress.android.fluxc.store.stats.insights

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.SummaryModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.SummaryRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.SummaryRestClient.SummaryResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.SummarySqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.SUMMARY_RESPONSE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class SummaryStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: SummaryRestClient
    @Mock lateinit var sqlUtils: SummarySqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var store: SummaryStore

    @Before
    fun setUp() {
        store = SummaryStore(restClient, sqlUtils, mapper, initCoroutineEngine())
    }

    @Test
    fun `returns summary per site`() = test {
        val fetchSummaryPayload = FetchStatsPayload(SUMMARY_RESPONSE)
        val forced = true
        whenever(restClient.fetchSummary(site, forced)).thenReturn(fetchSummaryPayload)
        val model = mock<SummaryModel>()
        whenever(mapper.map(SUMMARY_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchSummary(site, forced)

        Assertions.assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, SUMMARY_RESPONSE)
    }

    @Test
    fun `returns error when summary call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<SummaryResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchSummary(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchSummary(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns summary from db`() {
        whenever(sqlUtils.select(site)).thenReturn(SUMMARY_RESPONSE)
        val model = mock<SummaryModel>()
        whenever(mapper.map(SUMMARY_RESPONSE)).thenReturn(model)

        val result = store.getSummary(site)

        Assertions.assertThat(result).isEqualTo(model)
    }
}
