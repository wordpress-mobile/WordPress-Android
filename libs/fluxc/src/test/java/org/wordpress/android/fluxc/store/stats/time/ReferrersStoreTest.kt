package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8

@RunWith(MockitoJUnitRunner::class)
class ReferrersStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: ReferrersRestClient
    @Mock lateinit var sqlUtils: TimeStatsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: ReferrersStore
    @Before
    fun setUp() {
        store = ReferrersStore(
                restClient,
                sqlUtils,
                mapper,
                Unconfined
        )
    }

    @Test
    fun `returns referrers by day per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                REFERRERS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchReferrers(site, DAYS, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, DAYS, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, REFERRERS_RESPONSE, DAYS)
    }

    @Test
    fun `returns error when referrers by day call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<ReferrersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchReferrers(site, DAYS, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, DAYS, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns referrers by day from db`() {
        whenever(sqlUtils.selectReferrers(site, DAYS)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getReferrers(site, DAYS, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns referrers by week per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                REFERRERS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchReferrers(site, WEEKS, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, WEEKS, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, REFERRERS_RESPONSE, WEEKS)
    }

    @Test
    fun `returns error when referrers by week call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<ReferrersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchReferrers(site, WEEKS, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, WEEKS, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns referrers by week from db`() {
        whenever(sqlUtils.selectReferrers(site, WEEKS)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getReferrers(site, WEEKS, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns referrers by month per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                REFERRERS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchReferrers(site, MONTHS, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, MONTHS, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, REFERRERS_RESPONSE, MONTHS)
    }

    @Test
    fun `returns error when referrers by month call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<ReferrersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchReferrers(site, MONTHS, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, MONTHS, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns referrers by month from db`() {
        whenever(sqlUtils.selectReferrers(site, MONTHS)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getReferrers(site, MONTHS, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns referrers by year per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                REFERRERS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchReferrers(site, YEARS, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, YEARS, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, REFERRERS_RESPONSE, YEARS)
    }

    @Test
    fun `returns error when referrers by year call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<ReferrersResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchReferrers(site, YEARS, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchReferrers(site, PAGE_SIZE, YEARS, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns referrers by year from db`() {
        whenever(sqlUtils.selectReferrers(site, YEARS)).thenReturn(REFERRERS_RESPONSE)
        val model = mock<ReferrersModel>()
        whenever(mapper.map(REFERRERS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getReferrers(site, YEARS, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }
}
