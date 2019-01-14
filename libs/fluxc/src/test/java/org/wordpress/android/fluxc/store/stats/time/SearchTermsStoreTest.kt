package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.stats.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.stats.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsError
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8
private val DATE = Date(0)

@RunWith(MockitoJUnitRunner::class)
class SearchTermsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: SearchTermsRestClient
    @Mock lateinit var sqlUtils: TimeStatsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: SearchTermsStore
    @Before
    fun setUp() {
        store = SearchTermsStore(
                restClient,
                sqlUtils,
                mapper,
                Unconfined
        )
    }

    @Test
    fun `returns search terms per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                SEARCH_TERMS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchSearchTerms(site, DAYS, DATE, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<SearchTermsModel>()
        whenever(mapper.map(SEARCH_TERMS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchSearchTerms(site, PAGE_SIZE, DAYS, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, SEARCH_TERMS_RESPONSE, DAYS, DATE)
    }

    @Test
    fun `returns error when search terms call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<SearchTermsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchSearchTerms(site, DAYS, DATE, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchSearchTerms(site, PAGE_SIZE, DAYS, DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns search terms from db`() {
        whenever(sqlUtils.selectSearchTerms(site, DAYS, DATE)).thenReturn(SEARCH_TERMS_RESPONSE)
        val model = mock<SearchTermsModel>()
        whenever(mapper.map(SEARCH_TERMS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getSearchTerms(site, DAYS, PAGE_SIZE, DATE)

        assertThat(result).isEqualTo(model)
    }
}
