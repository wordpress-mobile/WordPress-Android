package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.SearchTermsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val ITEMS_TO_LOAD = 8
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)
private val DATE = Date(0)

@RunWith(MockitoJUnitRunner::class)
class SearchTermsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: SearchTermsRestClient
    @Mock lateinit var sqlUtils: SearchTermsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: SearchTermsStore
    @Before
    fun setUp() {
        store = SearchTermsStore(
                restClient,
                sqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns search terms per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                SEARCH_TERMS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchSearchTerms(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<SearchTermsModel>()
        whenever(mapper.map(SEARCH_TERMS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val responseModel = store.fetchSearchTerms(site, DAYS, LIMIT_MODE, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, SEARCH_TERMS_RESPONSE, DAYS, DATE, ITEMS_TO_LOAD)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, DATE, ITEMS_TO_LOAD)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(SEARCH_TERMS_RESPONSE)
        val model = mock<SearchTermsModel>()
        whenever(mapper.map(SEARCH_TERMS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val forced = false
        val responseModel = store.fetchSearchTerms(site, DAYS, LIMIT_MODE, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<Date>(), isNull())
    }

    @Test
    fun `returns error when search terms call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<SearchTermsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchSearchTerms(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchSearchTerms(site, DAYS, LIMIT_MODE, DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns search terms from db`() {
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(SEARCH_TERMS_RESPONSE)
        val model = mock<SearchTermsModel>()
        whenever(mapper.map(SEARCH_TERMS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val result = store.getSearchTerms(site, DAYS, LIMIT_MODE, DATE)

        assertThat(result).isEqualTo(model)
    }
}
