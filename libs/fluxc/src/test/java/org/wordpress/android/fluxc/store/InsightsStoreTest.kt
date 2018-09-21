package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse.StatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse.Discussion
import org.wordpress.android.fluxc.store.InsightsStore.FetchInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class InsightsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var insightsRestClient: InsightsRestClient
    private lateinit var store: InsightsStore
    private val siteId = 3L
    @Before
    fun setUp() {
        store = InsightsStore(insightsRestClient, Unconfined)
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `returns all time insights per site`() = test {
        val date = "1970"
        val visitors = 10
        val views = 15
        val posts = 20
        val viewsBestDay = "Monday"
        val viewsBestDayTotal = 25
        val fetchInsightsPayload = FetchInsightsPayload(
                AllTimeResponse(
                        date,
                        StatsResponse(visitors, views, posts, viewsBestDay, viewsBestDayTotal)
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val allTimeInsights = store.fetchAllTimeInsights(site, forced)

        assertNotNull(allTimeInsights.model)
        val model = allTimeInsights.model!!
        assertEquals(date, model.date)
        assertEquals(visitors, model.visitors)
        assertEquals(views, model.views)
        assertEquals(posts, model.posts)
        assertEquals(viewsBestDay, model.viewsBestDay)
        assertEquals(viewsBestDayTotal, model.viewsBestDayTotal)
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
    fun `returns most popular insights per site`() = test {
        val highestDayOfWeek = 10
        val highestHour = 15
        val highestDayPercent = 2.0
        val highestHourPercent = 5.0
        val fetchInsightsPayload = FetchInsightsPayload(
                MostPopularResponse(
                        highestDayOfWeek, highestHour, highestDayPercent, highestHourPercent
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val allTimeInsights = store.fetchMostPopularInsights(site, forced)

        assertNotNull(allTimeInsights.model)
        val model = allTimeInsights.model!!
        assertEquals(highestDayOfWeek, model.highestDayOfWeek)
        assertEquals(highestHour, model.highestHour)
        assertEquals(highestDayPercent, model.highestDayPercent)
        assertEquals(highestHourPercent, model.highestHourPercent)
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
    fun `returns latest post insights per site`() = test {
        val postsFound = 15
        val id: Long = 2
        val title = "title"
        val date = "date"
        val url = "url"
        val likeCount = 5
        val commentCount = 10
        val views = 20
        val latestPost = PostResponse(id, title, date, url, likeCount, Discussion(commentCount))
        val fetchInsightsPayload = FetchInsightsPayload(
                PostsResponse(
                        postsFound, listOf(latestPost)
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)
        whenever(insightsRestClient.fetchPostViewsForInsights(site, id, forced)).thenReturn(FetchInsightsPayload(
                PostViewsResponse(views)
        ))

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertNotNull(allTimeInsights.model)
        val model = allTimeInsights.model!!
        assertEquals(commentCount, model.postCommentCount)
        assertEquals(likeCount, model.postLikeCount)
        assertEquals(views, model.postViewsCount)
        assertEquals(date, model.postDate)
        assertEquals(id, model.postID)
        assertEquals(title, model.postTitle)
        assertEquals(url, model.postURL)
        assertEquals(siteId, model.siteId)
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
        val date = "date"
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
        val errorPayload = FetchInsightsPayload<PostViewsResponse>(StatsError(type, message))
        whenever(insightsRestClient.fetchPostViewsForInsights(site, id, forced)).thenReturn(errorPayload)

        val allTimeInsights = store.fetchLatestPostInsights(site, forced)

        assertNotNull(allTimeInsights.error)
        val error = allTimeInsights.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }
}
