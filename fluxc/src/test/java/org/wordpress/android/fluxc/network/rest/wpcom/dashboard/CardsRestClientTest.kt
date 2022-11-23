package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.TodaysStatsResponse
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsPayload
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
import org.wordpress.android.fluxc.test

/* DATE */

private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"

/* CARD TYPES */

private val CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)

/* ERRORS */
private const val JETPACK_DISABLED = "jetpack_disabled"
private const val UNAUTHORIZED = "unauthorized"

/* RESPONSE */

private val TODAYS_STATS_RESPONSE = TodaysStatsResponse(
        views = 100,
        visitors = 30,
        likes = 50,
        comments = 10
)

private val DRAFT_POST_RESPONSE_TWO = PostResponse(
        id = 708,
        title = "",
        content = "Draft Content 2",
        featuredImage = "https://test.blog/wp-content/uploads/2021/11/draft-featured-image-2.jpeg?w=200",
        date = "2021-11-02 15:47:42"
)

private val DRAFT_POST_RESPONSE_ONE = PostResponse(
        id = 659,
        title = "Draft Title 1",
        content = "Draft Content 1",
        featuredImage = null,
        date = "2021-10-27 12:25:57"
)

private val SCHEDULED_POST_RESPONSE_ONE = PostResponse(
        id = 762,
        title = "Scheduled Title 1",
        content = "",
        featuredImage = "https://test.blog/wp-content/uploads/2021/11/scheduled-featured-image-1.jpeg?w=200",
        date = "2021-12-26 23:00:33"
)

private val POSTS_RESPONSE = PostsResponse(
        hasPublished = true,
        draft = listOf(
                DRAFT_POST_RESPONSE_TWO,
                DRAFT_POST_RESPONSE_ONE
        ),
        scheduled = listOf(
                SCHEDULED_POST_RESPONSE_ONE
        )
)

private val CARDS_RESPONSE = CardsResponse(
        todaysStats = TODAYS_STATS_RESPONSE,
        posts = POSTS_RESPONSE
)

@RunWith(MockitoJUnitRunner::class)
class CardsRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: CardsRestClient

    private val siteId: Long = 1

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = CardsRestClient(
                wpComGsonRequestBuilder,
                dispatcher,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `when fetch cards gets triggered, then the correct request url is used`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_JSON)
        initFetchCards(data = getCardsResponseFromJsonString(json))

        restClient.fetchCards(site, CARD_TYPES)

        assertEquals(urlCaptor.firstValue, "$API_SITE_PATH/${site.siteId}/$API_DASHBOARD_CARDS_PATH")
    }

    @Test
    fun `given success call, when fetch cards gets triggered, then cards response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_JSON)
        initFetchCards(data = getCardsResponseFromJsonString(json))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertSuccess(CARDS_RESPONSE, result)
    }

    @Test
    fun `given timeout, when fetch cards gets triggered, then return cards timeout error`() = test {
        initFetchCards(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT)))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertError(CardsErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when fetch cards gets triggered, then return cards api error`() = test {
        initFetchCards(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertError(CardsErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when fetch cards gets triggered, then return cards invalid response error`() = test {
        initFetchCards(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE)))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertError(CardsErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given not authenticated, when fetch cards gets triggered, then return cards auth required error`() = test {
        initFetchCards(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED)))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertError(CardsErrorType.AUTHORIZATION_REQUIRED, result)
    }

    @Test
    fun `given unknown error, when fetch cards gets triggered, then return cards generic error`() = test {
        initFetchCards(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)))

        val result = restClient.fetchCards(site, CARD_TYPES)

        assertError(CardsErrorType.GENERIC_ERROR, result)
    }

    /* TODAY'S STATS CARD ERRORS */
    @Test
    fun `given jetpack disconn, when fetch cards triggered, then returns todays stats jetpack disconn card error`() =
            test {
                val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_WITH_ERRORS_JSON)
                initFetchCards(data = getCardsResponseFromJsonString(json))

                val result = restClient.fetchCards(site, CARD_TYPES)

                assertSuccessWithTodaysStatsError(TodaysStatsCardErrorType.JETPACK_DISCONNECTED, result)
            }

    @Test
    fun `given jetpack disabled, when fetch cards triggered, then returns todays stats jetpack disabled card error`() =
            test {
                val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_WITH_ERRORS_JSON)
                val data = getCardsResponseFromJsonString(json)
                        .copy(todaysStats = TodaysStatsResponse(error = JETPACK_DISABLED))
                initFetchCards(data = data)

                val result = restClient.fetchCards(site, CARD_TYPES)

                assertSuccessWithTodaysStatsError(TodaysStatsCardErrorType.JETPACK_DISABLED, result)
            }

    @Test
    fun `given stats unauthorized, when fetch cards triggered, then returns todays stats unauthorized card error`() =
            test {
                val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_WITH_ERRORS_JSON)
                val data = getCardsResponseFromJsonString(json)
                        .copy(todaysStats = TodaysStatsResponse(error = UNAUTHORIZED))
                initFetchCards(data = data)

                val result = restClient.fetchCards(site, CARD_TYPES)

                assertSuccessWithTodaysStatsError(TodaysStatsCardErrorType.UNAUTHORIZED, result)
            }

    /* POST CARD ERROR */
    @Test
    fun `given posts unauthorized error, when fetch cards triggered, then returns post card card error`() =
            test {
                val json = UnitTestUtils.getStringFromResourceFile(javaClass, DASHBOARD_CARDS_WITH_ERRORS_JSON)
                initFetchCards(data = getCardsResponseFromJsonString(json))

                val result = restClient.fetchCards(site, CARD_TYPES)

                assertSuccessWithPostCardError(result)
            }

    private fun CardsPayload<CardsResponse>.findTodaysStatsCardError(): TodaysStatsCardError? =
            this.response?.toCards()?.filterIsInstance(TodaysStatsCardModel::class.java)?.firstOrNull()?.error

    private fun CardsPayload<CardsResponse>.findPostCardError(): PostCardError? =
            this.response?.toCards()?.filterIsInstance(PostsCardModel::class.java)?.firstOrNull()?.error

    private fun getCardsResponseFromJsonString(json: String): CardsResponse {
        val responseType = object : TypeToken<CardsResponse>() {}.type
        return GsonBuilder().setDateFormat(DATE_FORMAT_PATTERN)
                .create().fromJson(json, responseType) as CardsResponse
    }

    private suspend fun initFetchCards(
        data: CardsResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CardsResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Success(nonNullData)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(CardsResponse::class.java),
                        eq(false),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: CardsResponse,
        actual: CardsPayload<CardsResponse>
    ) {
        with(actual) {
            assertEquals(site, this@CardsRestClientTest.site)
            assertFalse(isError)
            assertEquals(CardsPayload(expected), this)
        }
    }

    private fun assertError(
        expected: CardsErrorType,
        actual: CardsPayload<CardsResponse>
    ) {
        with(actual) {
            assertEquals(site, this@CardsRestClientTest.site)
            assertTrue(isError)
            assertEquals(expected, error.type)
            assertEquals(null, error.message)
        }
    }

    private fun assertSuccessWithTodaysStatsError(
        expected: TodaysStatsCardErrorType,
        actual: CardsPayload<CardsResponse>
    ) {
        with(actual) {
            assertEquals(site, this@CardsRestClientTest.site)
            assertFalse(isError)
            assertEquals(expected, findTodaysStatsCardError()?.type)
        }
    }

    private fun assertSuccessWithPostCardError(
        actual: CardsPayload<CardsResponse>
    ) {
        with(actual) {
            assertEquals(site, this@CardsRestClientTest.site)
            assertFalse(isError)
            assertEquals(PostCardErrorType.UNAUTHORIZED, findPostCardError()?.type)
        }
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_DASHBOARD_CARDS_PATH = "dashboard/cards-data/"

        private const val DASHBOARD_CARDS_JSON = "wp/dashboard/cards.json"
        private const val DASHBOARD_CARDS_WITH_ERRORS_JSON = "wp/dashboard/cards_with_errors.json"
    }
}
