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
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse.PostResponse.Discussion
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.AllTimeSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.CommentsInsightsSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.DetailedPostStatsSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.EmailFollowersSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.LatestPostDetailSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.PublicizeSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TagsSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TodayInsightsSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.WpComFollowersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.fluxc.store.stats.insights.PublicizeStore
import org.wordpress.android.fluxc.store.stats.insights.TagsStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8
private const val PAGE = 1
private val LOAD_MODE_INITIAL = PagedMode(PAGE_SIZE, false)
private val CACHE_MODE_TOP = LimitMode.Top(PAGE_SIZE)

@RunWith(MockitoJUnitRunner::class)
class InsightsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var allTimeInsightsRestClient: AllTimeInsightsRestClient
    @Mock lateinit var commentsRestClient: CommentsRestClient
    @Mock lateinit var followersRestClient: FollowersRestClient
    @Mock lateinit var latestPostInsightsRestClient: LatestPostInsightsRestClient
    @Mock lateinit var publicizeRestClient: PublicizeRestClient
    @Mock lateinit var tagsRestClient: TagsRestClient
    @Mock lateinit var todayInsightsRestClient: TodayInsightsRestClient
    @Mock lateinit var allTimeSqlUtils: AllTimeSqlUtils
    @Mock lateinit var commentInsightsSqlUtils: CommentsInsightsSqlUtils
    @Mock lateinit var wpComFollowersSqlUtils: WpComFollowersSqlUtils
    @Mock lateinit var emailFollowersSqlUtils: EmailFollowersSqlUtils
    @Mock lateinit var latestPostDetailSqlUtils: LatestPostDetailSqlUtils
    @Mock lateinit var detailedPostStatsSqlUtils: DetailedPostStatsSqlUtils
    @Mock lateinit var publicizeSqlUtils: PublicizeSqlUtils
    @Mock lateinit var tagsSqlUtils: TagsSqlUtils
    @Mock lateinit var todaySqlUtils: TodayInsightsSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private lateinit var allTimeStore: AllTimeInsightsStore
    private lateinit var commentsStore: CommentsStore
    private lateinit var followersStore: FollowersStore
    private lateinit var latestPostStore: LatestPostInsightsStore
    private lateinit var publicizeStore: PublicizeStore
    private lateinit var tagsStore: TagsStore
    private lateinit var todayStore: TodayInsightsStore
    @Before
    fun setUp() {
        allTimeStore = AllTimeInsightsStore(
                allTimeInsightsRestClient,
                allTimeSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        commentsStore = CommentsStore(
                commentsRestClient,
                commentInsightsSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        followersStore = FollowersStore(
                followersRestClient,
                wpComFollowersSqlUtils,
                emailFollowersSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        latestPostStore = LatestPostInsightsStore(
                latestPostInsightsRestClient,
                latestPostDetailSqlUtils,
                detailedPostStatsSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        publicizeStore = PublicizeStore(
                publicizeRestClient,
                publicizeSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        tagsStore = TagsStore(
                tagsRestClient,
                tagsSqlUtils,
                mapper,
                initCoroutineEngine()
        )
        todayStore = TodayInsightsStore(
                todayInsightsRestClient,
                todaySqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns all time insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                ALL_TIME_RESPONSE
        )
        val forced = true
        whenever(allTimeInsightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsAllTimeModel>()
        whenever(mapper.map(ALL_TIME_RESPONSE, site)).thenReturn(model)

        val responseModel = allTimeStore.fetchAllTimeInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(allTimeSqlUtils).insert(site, ALL_TIME_RESPONSE)
    }

    @Test
    fun `returns error when all time insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<AllTimeResponse>(StatsError(type, message))
        val forced = true
        whenever(allTimeInsightsRestClient.fetchAllTimeInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = allTimeStore.fetchAllTimeInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns all time insights from db`() {
        whenever(allTimeSqlUtils.select(site)).thenReturn(ALL_TIME_RESPONSE)
        val model = mock<InsightsAllTimeModel>()
        whenever(mapper.map(ALL_TIME_RESPONSE, site)).thenReturn(model)

        val result = allTimeStore.getAllTimeInsights(site)

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
        whenever(latestPostInsightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val viewsResponse = POST_STATS_RESPONSE
        whenever(latestPostInsightsRestClient.fetchPostStats(site, ID, forced)).thenReturn(
                FetchStatsPayload(
                        viewsResponse
                )
        )
        val model = mock<InsightsLatestPostModel>()
        whenever(mapper.map(
                LATEST_POST,
                POST_STATS_RESPONSE, site)).thenReturn(model)

        val responseModel = latestPostStore.fetchLatestPostInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(latestPostDetailSqlUtils).insert(site, LATEST_POST)
        verify(detailedPostStatsSqlUtils).insert(site, viewsResponse, postId = LATEST_POST.id)
    }

    @Test
    fun `returns latest post insights from db`() {
        whenever(latestPostDetailSqlUtils.select(site)).thenReturn(LATEST_POST)
        whenever(detailedPostStatsSqlUtils.select(site, LATEST_POST.id)).thenReturn(POST_STATS_RESPONSE)
        val model = mock<InsightsLatestPostModel>()
        whenever(mapper.map(
                LATEST_POST,
                POST_STATS_RESPONSE, site)).thenReturn(model)

        val result = latestPostStore.getLatestPostInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when latest post insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostsResponse>(StatsError(type, message))
        val forced = true
        whenever(latestPostInsightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = latestPostStore.fetchLatestPostInsights(site, forced)

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
        whenever(latestPostInsightsRestClient.fetchLatestPostForInsights(site, forced)).thenReturn(fetchInsightsPayload)

        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PostStatsResponse>(StatsError(type, message))
        whenever(latestPostInsightsRestClient.fetchPostStats(site, id, forced)).thenReturn(errorPayload)

        val responseModel = latestPostStore.fetchLatestPostInsights(site, forced)

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
        whenever(todayInsightsRestClient.fetchTimePeriodStats(site, DAYS, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<VisitsModel>()
        whenever(mapper.map(VISITS_RESPONSE)).thenReturn(model)

        val responseModel = todayStore.fetchTodayInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(todaySqlUtils).insert(site, VISITS_RESPONSE)
    }

    @Test
    fun `returns today stats from db`() {
        whenever(todaySqlUtils.select(site)).thenReturn(VISITS_RESPONSE)
        val model = mock<VisitsModel>()
        whenever(mapper.map(VISITS_RESPONSE)).thenReturn(model)

        val result = todayStore.getTodayInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when today stats call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<VisitResponse>(StatsError(type, message))
        val forced = true
        whenever(todayInsightsRestClient.fetchTimePeriodStats(site, DAYS, forced)).thenReturn(errorPayload)

        val responseModel = todayStore.fetchTodayInsights(site, forced)

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
        whenever(followersRestClient.fetchFollowers(site, WP_COM, PAGE, PAGE_SIZE, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = FollowersModel(0, emptyList(), false)
        whenever(wpComFollowersSqlUtils.selectAll(site)).thenReturn(listOf(FOLLOWERS_RESPONSE))
        whenever(mapper.mapAndMergeFollowersModels(listOf(FOLLOWERS_RESPONSE), WP_COM, LimitMode.All))
                .thenReturn(model)
        val responseModel = followersStore.fetchWpComFollowers(site, LOAD_MODE_INITIAL, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(wpComFollowersSqlUtils).insert(
                site,
                FOLLOWERS_RESPONSE,
                requestedItems = PAGE_SIZE,
                replaceExistingData = true
        )
    }

    @Test
    fun `returns email followers per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                FOLLOWERS_RESPONSE
        )
        val forced = true
        whenever(followersRestClient.fetchFollowers(site, EMAIL, PAGE, PAGE_SIZE, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = FollowersModel(0, emptyList(), false)
        whenever(emailFollowersSqlUtils.selectAll(site)).thenReturn(listOf(FOLLOWERS_RESPONSE))
        whenever(mapper.mapAndMergeFollowersModels(listOf(FOLLOWERS_RESPONSE), EMAIL, LimitMode.All))
                .thenReturn(model)
        val responseModel = followersStore.fetchEmailFollowers(site, LOAD_MODE_INITIAL, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(emailFollowersSqlUtils).insert(
                site,
                FOLLOWERS_RESPONSE,
                requestedItems = PAGE_SIZE,
                replaceExistingData = true
        )
    }

    @Test
    fun `returns WPCOM followers from db`() {
        val model = mock<FollowersModel>()
        whenever(wpComFollowersSqlUtils.selectAll(site)).thenReturn(listOf(FOLLOWERS_RESPONSE))
        whenever(mapper.mapAndMergeFollowersModels(listOf(FOLLOWERS_RESPONSE), WP_COM, LimitMode.Top(PAGE_SIZE)))
                .thenReturn(model)

        val result = followersStore.getWpComFollowers(site, CACHE_MODE_TOP)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns email followers from db`() {
        val model = mock<FollowersModel>()
        whenever(emailFollowersSqlUtils.selectAll(site)).thenReturn(listOf(FOLLOWERS_RESPONSE))
        whenever(mapper.mapAndMergeFollowersModels(listOf(FOLLOWERS_RESPONSE), EMAIL, LimitMode.Top(PAGE_SIZE)))
                .thenReturn(model)

        val result = followersStore.getEmailFollowers(site, CACHE_MODE_TOP)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when WPCOM followers call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<FollowersResponse>(StatsError(type, message))
        val forced = true
        whenever(followersRestClient.fetchFollowers(site, WP_COM, PAGE, PAGE_SIZE, forced)).thenReturn(errorPayload)

        val responseModel = followersStore.fetchWpComFollowers(site, LOAD_MODE_INITIAL, forced)

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
        whenever(followersRestClient.fetchFollowers(site, EMAIL, PAGE, PAGE_SIZE, forced)).thenReturn(errorPayload)

        val responseModel = followersStore.fetchEmailFollowers(site, LOAD_MODE_INITIAL, forced)

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
        whenever(commentsRestClient.fetchTopComments(site, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<CommentsModel>()
        whenever(mapper.map(TOP_COMMENTS_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val responseModel = commentsStore.fetchComments(site, LimitMode.Top(PAGE_SIZE), forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(commentInsightsSqlUtils).insert(site, TOP_COMMENTS_RESPONSE, requestedItems = PAGE_SIZE)
    }

    @Test
    fun `returns top comments from db`() {
        whenever(commentInsightsSqlUtils.select(site)).thenReturn(TOP_COMMENTS_RESPONSE)
        val model = mock<CommentsModel>()
        whenever(mapper.map(TOP_COMMENTS_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val result = commentsStore.getComments(site, LimitMode.Top(PAGE_SIZE))

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when top comments call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<CommentsResponse>(StatsError(type, message))
        val forced = true
        whenever(commentsRestClient.fetchTopComments(site, forced)).thenReturn(errorPayload)

        val responseModel = commentsStore.fetchComments(site, LimitMode.Top(PAGE_SIZE), forced)

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
        whenever(tagsRestClient.fetchTags(site, PAGE_SIZE + 1, forced = forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<TagsModel>()
        whenever(mapper.map(TAGS_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val responseModel = tagsStore.fetchTags(site, LimitMode.Top(PAGE_SIZE), forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(tagsSqlUtils).insert(site, TAGS_RESPONSE, requestedItems = PAGE_SIZE)
    }

    @Test
    fun `returns tags and categories from db`() {
        whenever(tagsSqlUtils.select(site)).thenReturn(TAGS_RESPONSE)
        val model = mock<TagsModel>()
        whenever(mapper.map(TAGS_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val result = tagsStore.getTags(site, LimitMode.Top(PAGE_SIZE))

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when tags and categories call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<TagsResponse>(StatsError(type, message))
        val forced = true
        whenever(tagsRestClient.fetchTags(site, PAGE_SIZE + 1, forced = forced)).thenReturn(errorPayload)

        val responseModel = tagsStore.fetchTags(site, LimitMode.Top(PAGE_SIZE), forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns publicize data per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                PUBLICIZE_RESPONSE
        )
        val forced = true
        whenever(publicizeRestClient.fetchPublicizeData(site, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<PublicizeModel>()
        whenever(mapper.map(PUBLICIZE_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val responseModel = publicizeStore.fetchPublicizeData(site, LimitMode.Top(PAGE_SIZE), forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(publicizeSqlUtils).insert(site, PUBLICIZE_RESPONSE)
    }

    @Test
    fun `returns publicize data from db`() {
        whenever(publicizeSqlUtils.select(site)).thenReturn(PUBLICIZE_RESPONSE)
        val model = mock<PublicizeModel>()
        whenever(mapper.map(PUBLICIZE_RESPONSE, LimitMode.Top(PAGE_SIZE))).thenReturn(model)

        val result = publicizeStore.getPublicizeData(site, LimitMode.Top(PAGE_SIZE))

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns error when publicize data call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<PublicizeResponse>(StatsError(type, message))
        val forced = true
        whenever(publicizeRestClient.fetchPublicizeData(site, forced)).thenReturn(errorPayload)

        val responseModel = publicizeStore.fetchPublicizeData(site, LimitMode.Top(PAGE_SIZE), forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }
}
