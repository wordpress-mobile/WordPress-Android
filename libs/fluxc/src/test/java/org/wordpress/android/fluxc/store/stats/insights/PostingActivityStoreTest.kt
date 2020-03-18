package org.wordpress.android.fluxc.store.stats.insights

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.PostingActivitySqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.POSTING_ACTIVITY_RESPONSE
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PostingActivityStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: PostingActivityRestClient
    @Mock lateinit var sqlUtils: PostingActivitySqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var store: PostingActivityStore
    private val startDate = Day(2018, 1, 1)
    private val endDate = Day(2019, 1, 1)
    @Before
    fun setUp() {
        store = PostingActivityStore(
                restClient,
                sqlUtils,
                initCoroutineEngine(),
                mapper
        )
    }

    @Test
    fun `fetches posting activity per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                POSTING_ACTIVITY_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchPostingActivity(site, startDate, endDate, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<PostingActivityModel>()
        whenever(mapper.map(POSTING_ACTIVITY_RESPONSE, startDate, endDate)).thenReturn(model)

        val responseModel = store.fetchPostingActivity(site, startDate, endDate, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, POSTING_ACTIVITY_RESPONSE)
    }

    @Test
    fun `fetches error when posting activity call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostingActivityResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchPostingActivity(site, startDate, endDate, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchPostingActivity(site, startDate, endDate, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `loads posting activity per site from DB`() = test {
        whenever(sqlUtils.select(site)).thenReturn(POSTING_ACTIVITY_RESPONSE)
        val model = mock<PostingActivityModel>()
        whenever(mapper.map(POSTING_ACTIVITY_RESPONSE, startDate, endDate)).thenReturn(model)

        val dbModel = store.getPostingActivity(site, startDate, endDate)

        assertThat(dbModel).isEqualTo(model)
    }
}
