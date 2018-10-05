package org.wordpress.android.fluxc.network.rest.wpcom.stats

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class InsightsRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var insightsRestClient: InsightsRestClient
    private val siteId: Long = 12
    private val postId: Long = 1

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        insightsRestClient = InsightsRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `returns all time success response`() = test {
        val response = mock<AllTimeResponse>()
        initAllTimeResponse(response)

        val allTimeInsights = insightsRestClient.fetchAllTimeInsights(site, false)

        assertNotNull(allTimeInsights.response)
        assertEquals(response, allTimeInsights.response)
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/", urlCaptor.lastValue)
    }

    @Test
    fun `returns all time error response`() = test {
        val errorMessage = "message"
        initAllTimeResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val allTimeInsights = insightsRestClient.fetchAllTimeInsights(site, false)

        assertNotNull(allTimeInsights.error)
        assertEquals(API_ERROR, allTimeInsights.error.type)
        assertEquals(errorMessage, allTimeInsights.error.message)
    }

    @Test
    fun `returns most popular success response`() = test {
        val response = mock<MostPopularResponse>()
        initMostPopularResponse(response)

        val allTimeInsights = insightsRestClient.fetchMostPopularInsights(site, false)

        assertNotNull(allTimeInsights.response)
        assertEquals(response, allTimeInsights.response)
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/insights/", urlCaptor.lastValue)
    }

    @Test
    fun `returns most popular error response`() = test {
        val errorMessage = "message"
        initMostPopularResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val allTimeInsights = insightsRestClient.fetchMostPopularInsights(site, false)

        assertNotNull(allTimeInsights.error)
        assertEquals(API_ERROR, allTimeInsights.error.type)
        assertEquals(errorMessage, allTimeInsights.error.message)
    }

    @Test
    fun `returns latest post success response`() = test {
        val response = mock<PostsResponse>()
        initLatestPostResponse(response)

        val allTimeInsights = insightsRestClient.fetchLatestPostForInsights(site, false)

        assertNotNull(allTimeInsights.response)
        assertEquals(response, allTimeInsights.response)
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/12/posts/", urlCaptor.lastValue)
    }

    @Test
    fun `returns latest post error response`() = test {
        val errorMessage = "message"
        initLatestPostResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val allTimeInsights = insightsRestClient.fetchLatestPostForInsights(site, false)

        assertNotNull(allTimeInsights.error)
        assertEquals(API_ERROR, allTimeInsights.error.type)
        assertEquals(errorMessage, allTimeInsights.error.message)
    }

    @Test
    fun `returns posts view success response`() = test {
        val response = mock<PostViewsResponse>()
        initPostsViewResponse(response)

        val allTimeInsights = insightsRestClient.fetchPostViewsForInsights(site, postId, false)

        assertNotNull(allTimeInsights.response)
        assertEquals(response, allTimeInsights.response)
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/post/1/", urlCaptor.lastValue)
    }

    @Test
    fun `returns posts view error response`() = test {
        val errorMessage = "message"
        initPostsViewResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val allTimeInsights = insightsRestClient.fetchPostViewsForInsights(site, postId, false)

        assertNotNull(allTimeInsights.error)
        assertEquals(API_ERROR, allTimeInsights.error.type)
        assertEquals(errorMessage, allTimeInsights.error.message)
    }

    private suspend fun initAllTimeResponse(
        data: AllTimeResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<AllTimeResponse> {
        return initResponse(AllTimeResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initMostPopularResponse(
        data: MostPopularResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<MostPopularResponse> {
        return initResponse(MostPopularResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initLatestPostResponse(
        data: PostsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<PostsResponse> {
        return initResponse(PostsResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initPostsViewResponse(
        data: PostViewsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<PostViewsResponse> {
        return initResponse(PostViewsResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error<T>(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(insightsRestClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(kclass),
                        eq(true),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
