package org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient.SubscribersResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class SubscribersRestClientTest {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder

    @Mock
    private lateinit var site: SiteModel

    @Mock
    private lateinit var requestQueue: RequestQueue

    @Mock
    private lateinit var accessToken: AccessToken

    @Mock
    private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: SubscribersRestClient
    private val siteId = 12L
    private val quantity = 30
    private val currentDateValue = "2022-10-10"

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = SubscribersRestClient(
            dispatcher,
            wpComGsonRequestBuilder,
            null,
            requestQueue,
            accessToken, userAgent
        )
    }

    @Test
    fun `returns subscribers per day success response`() = test { testSuccessResponse(DAYS) }

    @Test
    fun `returns subscribers per day error response`() = test { testErrorResponse(DAYS) }

    @Test
    fun `returns subscribers per week success response`() = test { testSuccessResponse(WEEKS) }

    @Test
    fun `returns subscribers per week error response`() = test { testErrorResponse(WEEKS) }

    @Test
    fun `returns subscribers per month success response`() = test { testSuccessResponse(MONTHS) }

    @Test
    fun `returns subscribers per month error response`() = test { testErrorResponse(MONTHS) }

    @Test
    fun `returns subscribers per year success response`() = test { testSuccessResponse(YEARS) }

    @Test
    fun `returns subscribers per year error response`() = test { testErrorResponse(YEARS) }

    private suspend fun testSuccessResponse(granularity: StatsGranularity) {
        val response = mock<SubscribersResponse>()
        initSubscribersResponse(response)

        val responseModel = restClient.fetchSubscribers(site, granularity, quantity, currentDateValue, false)

        assertThat(responseModel.response).isNotNull()
        assertThat(responseModel.response).isEqualTo(response)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/subscribers/")
        assertThat(paramsCaptor.lastValue).isEqualTo(
            mapOf("quantity" to quantity.toString(), "unit" to granularity.toString(), "date" to currentDateValue)
        )
    }

    private suspend fun testErrorResponse(period: StatsGranularity) {
        val errorMessage = "message"
        initSubscribersResponse(
            error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR, errorMessage, VolleyError(errorMessage)))
        )

        val responseModel = restClient.fetchSubscribers(site, period, quantity, currentDateValue, false)

        assertThat(responseModel.error).isNotNull()
        assertThat(responseModel.error.type).isEqualTo(API_ERROR)
        assertThat(responseModel.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initSubscribersResponse(
        data: SubscribersResponse? = null,
        error: WPComGsonNetworkError? = null
    ) = initResponse(SubscribersResponse::class.java, data ?: mock(), error)

    private suspend fun <T> initResponse(
        clazz: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null,
        cachingEnabled: Boolean = false
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data)
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(clazz),
                eq(cachingEnabled),
                any(),
                eq(false),
                customGsonBuilder = anyOrNull()
            )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
