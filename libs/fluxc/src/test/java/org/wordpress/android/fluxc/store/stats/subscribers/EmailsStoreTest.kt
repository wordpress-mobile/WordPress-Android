package org.wordpress.android.fluxc.store.stats.subscribers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.PostsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.EmailsSummaryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.SortField
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.EmailsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class EmailsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: EmailsRestClient
    @Mock lateinit var sqlUtils: EmailsSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var store: EmailsStore
    private val limitMode = LimitMode.Top(10)

    @Before
    fun setUp() {
        store = EmailsStore(restClient, sqlUtils, mapper, initCoroutineEngine())
    }

    @Test
    fun `caches fetched emails keyed by sort field`() = test {
        val response = mock<EmailsSummaryResponse>()
        val model = mock<PostsModel>()
        whenever(restClient.fetchEmailsSummary(site, limitMode.limit, SortField.POST_DATE, false))
            .thenReturn(FetchStatsPayload(response))
        whenever(mapper.map(response, limitMode, SortField.POST_DATE)).thenReturn(model)

        val result = store.fetchEmails(site, limitMode, SortField.POST_DATE, forced = false)

        assertThat(result.model).isEqualTo(model)
        verify(sqlUtils).insert(site, response, requestedItems = limitMode.limit, cacheKey = "post_date")
    }

    @Test
    fun `returns cached emails for the requested sort field without hitting the network`() = test {
        val response = mock<EmailsSummaryResponse>()
        val model = mock<PostsModel>()
        whenever(sqlUtils.hasFreshRequest(site, limitMode.limit, cacheKey = "opens")).thenReturn(true)
        whenever(sqlUtils.select(site, cacheKey = "opens")).thenReturn(response)
        whenever(mapper.map(response, limitMode, SortField.OPENS)).thenReturn(model)

        val result = store.fetchEmails(site, limitMode, SortField.OPENS, forced = false)

        assertThat(result.model).isEqualTo(model)
        assertThat(result.cached).isTrue()
        verify(restClient, never()).fetchEmailsSummary(site, limitMode.limit, SortField.OPENS, false)
    }

    @Test
    fun `reads the cache slot matching the requested sort field`() = test {
        val response = mock<EmailsSummaryResponse>()
        val model = mock<PostsModel>()
        whenever(sqlUtils.select(site, cacheKey = "post_date")).thenReturn(response)
        whenever(mapper.map(response, limitMode, SortField.POST_DATE)).thenReturn(model)

        val result = store.getEmails(site, limitMode, SortField.POST_DATE)

        assertThat(result).isEqualTo(model)
    }
}
