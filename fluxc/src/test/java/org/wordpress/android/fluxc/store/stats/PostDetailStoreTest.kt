package org.wordpress.android.fluxc.store.stats

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
import org.wordpress.android.fluxc.model.stats.PostDetailStatsMapper
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.DetailedPostStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PostDetailStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: LatestPostInsightsRestClient
    @Mock lateinit var sqlUtils: DetailedPostStatsSqlUtils
    @Mock lateinit var mapper: PostDetailStatsMapper
    private lateinit var store: PostDetailStore
    private val postId: Long = 1L

    @Before
    fun setUp() {
        store = PostDetailStore(
                restClient,
                sqlUtils,
                initCoroutineEngine(),
                mapper
        )
    }

    @Test
    fun `returns post detail stats per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                POST_STATS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchPostStats(site, postId, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<PostDetailStatsModel>()
        whenever(mapper.map(POST_STATS_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchPostDetail(site, postId, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, POST_STATS_RESPONSE, postId = postId)
    }

    @Test
    fun `returns error when post detail stats call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostStatsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchPostStats(site, postId, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchPostDetail(site, postId, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns post detail from DB`() = test {
        whenever(sqlUtils.select(site, postId)).thenReturn(POST_STATS_RESPONSE)
        val model = mock<PostDetailStatsModel>()
        whenever(mapper.map(POST_STATS_RESPONSE)).thenReturn(model)

        val dbModel = store.getPostDetail(site, postId)

        assertThat(dbModel).isEqualTo(model)
    }
}
