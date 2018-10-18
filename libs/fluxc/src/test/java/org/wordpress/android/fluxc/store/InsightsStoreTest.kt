package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
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
    private lateinit var store: InsightsStore
    private val siteId = 3L
    @Before
    fun setUp() {
        store = InsightsStore(insightsRestClient, sqlUtils, Unconfined)
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `returns all time insights per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                ALL_TIME_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val allTimeInsights = store.fetchAllTimeInsights(site, forced)

        assertAllTimeInsights(allTimeInsights.model)
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

        val result = store.getAllTimeInsights(site)

        assertAllTimeInsights(result)
    }

    private fun assertAllTimeInsights(model: InsightsAllTimeModel?) {
        assertNotNull(model)
        model?.let {
            assertEquals(DATE, model.date)
            assertEquals(VISITORS, model.visitors)
            assertEquals(VIEWS, model.views)
            assertEquals(POSTS, model.posts)
            assertEquals(VIEWS_BEST_DAY, model.viewsBestDay)
            assertEquals(VIEWS_BEST_DAY_TOTAL, model.viewsBestDayTotal)
        }
    }

    @Test
    fun `returns most popular insights per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val allTimeInsights = store.fetchMostPopularInsights(site, forced)

        assertMostPopularInsights(allTimeInsights.model)
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

        val result = store.getMostPopularInsights(site)

        assertMostPopularInsights(result)
    }

    private fun assertMostPopularInsights(model: InsightsMostPopularModel?) {
        assertNotNull(model)
        model?.let {
            assertEquals(HIGHEST_DAY_OF_WEEK, model.highestDayOfWeek)
            assertEquals(HIGHEST_HOUR, model.highestHour)
            assertEquals(HIGHEST_DAY_PERCENT, model.highestDayPercent)
            assertEquals(HIGHEST_HOUR_PERCENT, model.highestHourPercent)
        }
    }

    @Test
    fun `returns latest post insights per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                PostsResponse(
                        POSTS_FOUND, listOf(LATEST_POST)
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val viewsResponse = POST_STATS_RESPONSE
        whenever(insightsRestClient.fetchPostStats(site, ID, forced)).thenReturn(
                FetchInsightsPayload(
                        viewsResponse
                )
        )

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertLatestPostInsights(allTimeInsights.model)
        verify(sqlUtils).insert(site, LATEST_POST)
        verify(sqlUtils).insert(site, viewsResponse)
    }

    @Test
    fun `returns latest post insights from db`() {
        whenever(sqlUtils.selectLatestPostDetail(site)).thenReturn(LATEST_POST)
        whenever(sqlUtils.selectLatestPostStats(site)).thenReturn(POST_STATS_RESPONSE)

        val result = store.getLatestPostInsights(site)

        assertLatestPostInsights(result)
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

    private fun assertLatestPostInsights(model: InsightsLatestPostModel?) {
        assertNotNull(model)
        model?.let {
            assertEquals(COMMENT_COUNT, model.postCommentCount)
            assertEquals(LIKE_COUNT, model.postLikeCount)
            assertEquals(VIEWS, model.postViewsCount)
            assertEquals(DATE, model.postDate)
            assertEquals(ID, model.postId)
            assertEquals(TITLE, model.postTitle)
            assertEquals(URL, model.postURL)
            assertEquals(siteId, model.siteId)
        }
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
