package org.wordpress.android.fluxc.store.stats.insights

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient.MostPopularResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.MostPopularSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.MOST_POPULAR_RESPONSE
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class MostPopularInsightsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: MostPopularRestClient
    @Mock lateinit var sqlUtils: MostPopularSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var store: MostPopularInsightsStore
    @Before
    fun setUp() {
        store = MostPopularInsightsStore(
                restClient,
                sqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns most popular insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        Assertions.assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns error when most popular insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<MostPopularResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns most popular insights from db`() {
        whenever(sqlUtils.select(site)).thenReturn(MOST_POPULAR_RESPONSE)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val result = store.getMostPopularInsights(site)

        Assertions.assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns years insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<YearsInsightsModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchYearsInsights(site, forced)

        Assertions.assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns years insights from db`() {
        whenever(sqlUtils.select(site)).thenReturn(MOST_POPULAR_RESPONSE)
        val model = mock<YearsInsightsModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE)).thenReturn(model)

        val result = store.getYearsInsights(site)

        Assertions.assertThat(result).isEqualTo(model)
    }
}
