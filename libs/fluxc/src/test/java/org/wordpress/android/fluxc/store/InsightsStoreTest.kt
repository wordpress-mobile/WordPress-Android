package org.wordpress.android.fluxc.store

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
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse.Discussion
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.InsightsStore.FetchInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class InsightsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var insightsRestClient: InsightsRestClient
    @Mock lateinit var sqlUtils: InsightsSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var store: InsightsStore
    @Before
    fun setUp() {
        store = InsightsStore(insightsRestClient, sqlUtils, mapper, Unconfined)
    }

    @Test
    fun `returns all time insights per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                ALL_TIME_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsAllTimeModel>()
        whenever(mapper.map(ALL_TIME_RESPONSE, site)).thenReturn(model)

        val allTimeInsights = store.fetchAllTimeInsights(site, forced)

        assertThat(allTimeInsights.model).isEqualTo(model)
        verify(sqlUtils).insert(site, ALL_TIME_RESPONSE)
    }

    @Test
    fun `returns error when all time insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchInsightsPayload<AllTimeResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(errorPayload)

        val allTimeInsights = store.fetchAllTimeInsights(site, forced)

        assertNotNull(allTimeInsights.error)
        val error = allTimeInsights.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns all time insights from db`() {
        whenever(sqlUtils.selectAllTimeInsights(site)).thenReturn(ALL_TIME_RESPONSE)
        val model = mock<InsightsAllTimeModel>()
        whenever(mapper.map(ALL_TIME_RESPONSE, site)).thenReturn(model)

        val result = store.getAllTimeInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns most popular insights per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)


        val allTimeInsights = store.fetchMostPopularInsights(site, forced)

        assertThat(allTimeInsights.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns error when most popular insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchInsightsPayload<MostPopularResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(errorPayload)

        val allTimeInsights = store.fetchMostPopularInsights(site, forced)

        assertNotNull(allTimeInsights.error)
        val error = allTimeInsights.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns most popular insights from db`() {
        whenever(sqlUtils.selectMostPopularInsights(site)).thenReturn(MOST_POPULAR_RESPONSE)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val result = store.getMostPopularInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns latest post insights per site`() = test {
        val postsResponse = PostsResponse(
                POSTS_FOUND, listOf(LATEST_POST)
        )
        val fetchInsightsPayload = FetchInsightsPayload(
                postsResponse
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val viewsResponse = POST_STATS_RESPONSE
        whenever(insightsRestClient.fetchPostStats(site, ID, forced)).thenReturn(
                FetchInsightsPayload(
                        viewsResponse
                )
        )
        val model = mock<InsightsLatestPostModel>()
        whenever(mapper.map(LATEST_POST, POST_STATS_RESPONSE, site)).thenReturn(model)

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertThat(allTimeInsights.model).isEqualTo(model)
        verify(sqlUtils).insert(site, LATEST_POST)
        verify(sqlUtils).insert(site, viewsResponse)
    }

    @Test
    fun `returns latest post insights from db`() {
        whenever(sqlUtils.selectLatestPostDetail(site)).thenReturn(LATEST_POST)
        whenever(sqlUtils.selectLatestPostStats(site)).thenReturn(POST_STATS_RESPONSE)
        val model = mock<InsightsLatestPostModel>()
        whenever(mapper.map(LATEST_POST, POST_STATS_RESPONSE, site)).thenReturn(model)

        val result = store.getLatestPostInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when latest post insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchInsightsPayload<PostsResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(errorPayload)

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertNotNull(allTimeInsights.error)
        val error = allTimeInsights.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns error when latest post views insights call fail`() = test {
        val postsFound = 15
        val id: Long = 2
        val title = "title"
        val date = Date(10)
        val url = "url"
        val likeCount = 5
        val commentCount = 10
        val latestPost = PostResponse(id, title, date, url, likeCount, Discussion(commentCount))
        val fetchInsightsPayload = FetchInsightsPayload(
                PostsResponse(
                        postsFound, listOf(latestPost)
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchInsightsPayload<PostStatsResponse>(StatsError(type, message))
        whenever(insightsRestClient.fetchPostStats(site, id, forced)).thenReturn(errorPayload)

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertNotNull(allTimeInsights.error)
        val error = allTimeInsights.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }
}
