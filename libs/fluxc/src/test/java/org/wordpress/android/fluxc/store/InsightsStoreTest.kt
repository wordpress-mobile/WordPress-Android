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
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse.Discussion
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8

@RunWith(MockitoJUnitRunner::class)
class InsightsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var insightsRestClient: InsightsRestClient
    @Mock lateinit var sqlUtils: InsightsSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    @Mock lateinit var timeProvider: CurrentTimeProvider
    private lateinit var store: InsightsStore
    private val currentDate = Date(10)
    @Before
    fun setUp() {
        store = InsightsStore(insightsRestClient, sqlUtils, mapper, timeProvider, Unconfined)
        whenever(timeProvider.currentDate).thenReturn(currentDate)
    }

    @Test
    fun `returns all time insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                ALL_TIME_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsAllTimeModel>()
        whenever(mapper.map(ALL_TIME_RESPONSE, site)).thenReturn(model)

        val responseModel = store.fetchAllTimeInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, ALL_TIME_RESPONSE)
    }

    @Test
    fun `returns error when all time insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<AllTimeResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchAllTimeInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
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
        val fetchInsightsPayload = FetchStatsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns error when most popular insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<MostPopularResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchMostPopularInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
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
        val fetchInsightsPayload = FetchStatsPayload(
                postsResponse
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val viewsResponse = POST_STATS_RESPONSE
        whenever(insightsRestClient.fetchPostStats(site, ID, forced)).thenReturn(
                FetchStatsPayload(
                        viewsResponse
                )
        )
        val model = mock<InsightsLatestPostModel>()
        whenever(mapper.map(LATEST_POST, POST_STATS_RESPONSE, site)).thenReturn(model)

        val responseModel = store.fetchLatestPostInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
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
        val errorPayload = FetchStatsPayload<PostsResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchLatestPostInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
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
        val fetchInsightsPayload = FetchStatsPayload(
                PostsResponse(
                        postsFound, listOf(latestPost)
                )
        )
        val forced = true
        whenever(insightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostStatsResponse>(StatsError(type, message))
        whenever(insightsRestClient.fetchPostStats(site, id, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchLatestPostInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns today stats per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                VISITS_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchTimePeriodStats(site, DAYS, currentDate, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<VisitsModel>()
        whenever(mapper.map(VISITS_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchTodayInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, VISITS_RESPONSE)
    }

    @Test
    fun `returns today stats from db`() {
        whenever(sqlUtils.selectTodayInsights(site)).thenReturn(VISITS_RESPONSE)
        val model = mock<VisitsModel>()
        whenever(mapper.map(VISITS_RESPONSE)).thenReturn(model)

        val result = store.getTodayInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when today stats call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<VisitResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchTimePeriodStats(site, DAYS, currentDate, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchTodayInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns WPCOM followers per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                FOLLOWERS_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchFollowers(site, WP_COM, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<FollowersModel>()
        whenever(mapper.map(FOLLOWERS_RESPONSE, WP_COM, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchWpComFollowers(site, PAGE_SIZE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, FOLLOWERS_RESPONSE, WP_COM)
    }

    @Test
    fun `returns email followers per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                FOLLOWERS_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchFollowers(site, EMAIL, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<FollowersModel>()
        whenever(mapper.map(FOLLOWERS_RESPONSE, EMAIL, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchEmailFollowers(site, PAGE_SIZE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, FOLLOWERS_RESPONSE, EMAIL)
    }

    @Test
    fun `returns WPCOM followers from db`() {
        whenever(sqlUtils.selectFollowers(site, WP_COM)).thenReturn(FOLLOWERS_RESPONSE)
        val model = mock<FollowersModel>()
        whenever(mapper.map(FOLLOWERS_RESPONSE, WP_COM, PAGE_SIZE)).thenReturn(model)

        val result = store.getWpComFollowers(site, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns email followers from db`() {
        whenever(sqlUtils.selectFollowers(site, EMAIL)).thenReturn(FOLLOWERS_RESPONSE)
        val model = mock<FollowersModel>()
        whenever(mapper.map(FOLLOWERS_RESPONSE, EMAIL, PAGE_SIZE)).thenReturn(model)

        val result = store.getEmailFollowers(site, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when WPCOM followers call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<FollowersResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchFollowers(site, WP_COM, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchWpComFollowers(site, PAGE_SIZE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns error when email followers call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<FollowersResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchFollowers(site, EMAIL, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchEmailFollowers(site, PAGE_SIZE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns top comments per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                TOP_COMMENTS_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchTopComments(site, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<CommentsModel>()
        whenever(mapper.map(TOP_COMMENTS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchComments(site, PAGE_SIZE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, TOP_COMMENTS_RESPONSE)
    }

    @Test
    fun `returns top comments from db`() {
        whenever(sqlUtils.selectCommentInsights(site)).thenReturn(TOP_COMMENTS_RESPONSE)
        val model = mock<CommentsModel>()
        whenever(mapper.map(TOP_COMMENTS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getComments(site, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when top comments call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<CommentsResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchTopComments(site, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchComments(site, PAGE_SIZE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns tags and categories per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                TAGS_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchTags(site, PAGE_SIZE + 1, forced = forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<TagsModel>()
        whenever(mapper.map(TAGS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchTags(site, PAGE_SIZE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, TAGS_RESPONSE)
    }

    @Test
    fun `returns tags and categories from db`() {
        whenever(sqlUtils.selectTags(site)).thenReturn(TAGS_RESPONSE)
        val model = mock<TagsModel>()
        whenever(mapper.map(TAGS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getTags(site, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when tags and categories call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<TagsResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchTags(site, PAGE_SIZE + 1, forced = forced)).thenReturn(errorPayload)

        val responseModel = store.fetchTags(site, PAGE_SIZE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns publicize data per site`() = test {
        val fetchInsightsPayload = FetchInsightsPayload(
                PUBLICIZE_RESPONSE
        )
        val forced = true
        whenever(insightsRestClient.fetchPublicizeData(site, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<PublicizeModel>()
        whenever(mapper.map(PUBLICIZE_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchPublicizeData(site, PAGE_SIZE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, PUBLICIZE_RESPONSE)
    }

    @Test
    fun `returns publicize data from db`() {
        whenever(sqlUtils.selectPublicizeInsights(site)).thenReturn(PUBLICIZE_RESPONSE)
        val model = mock<PublicizeModel>()
        whenever(mapper.map(PUBLICIZE_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getPublicizeData(site, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when publicize data call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchInsightsPayload<PublicizeResponse>(StatsError(type, message))
        val forced = true
        whenever(insightsRestClient.fetchPublicizeData(site, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchPublicizeData(site, PAGE_SIZE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }
}
