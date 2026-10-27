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
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.EmailsSummaryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.SortField
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class EmailsRestClientTest {
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
    private lateinit var restClient: EmailsRestClient
    private val siteId = 12L
    private val quantity = 30

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = EmailsRestClient(
            dispatcher,
            wpComGsonRequestBuilder,
            null,
            requestQueue,
            accessToken, userAgent
        )
    }

    @Test
    fun `sends post_date sort field over the alltime period for POST_DATE sort`() = test {
        val response = mock<EmailsSummaryResponse>()
        initEmailsResponse(response)

        val responseModel = restClient.fetchEmailsSummary(site, quantity, SortField.POST_DATE, false)

        assertThat(responseModel.response).isNotNull()
        assertThat(responseModel.response).isEqualTo(response)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/stats/emails/summary/")
        assertThat(paramsCaptor.lastValue).isEqualTo(
            mapOf(
                "period" to "alltime",
                "quantity" to quantity.toString(),
                "sort_field" to "post_date",
                "sort_order" to "desc"
            )
        )
    }

    @Test
    fun `sends opens sort field for OPENS sort`() = test {
        val response = mock<EmailsSummaryResponse>()
        initEmailsResponse(response)

        restClient.fetchEmailsSummary(site, quantity, SortField.OPENS, false)

        assertThat(paramsCaptor.lastValue).isEqualTo(
            mapOf(
                "period" to "alltime",
                "quantity" to quantity.toString(),
                "sort_field" to "opens",
                "sort_order" to "desc"
            )
        )
    }

    @Test
    fun `returns error response`() = test {
        val errorMessage = "message"
        initEmailsResponse(
            error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR, errorMessage, VolleyError(errorMessage)))
        )

        val responseModel = restClient.fetchEmailsSummary(site, quantity, SortField.POST_DATE, false)

        assertThat(responseModel.error).isNotNull()
        assertThat(responseModel.error.type).isEqualTo(API_ERROR)
        assertThat(responseModel.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initEmailsResponse(
        data: EmailsSummaryResponse? = null,
        error: WPComGsonNetworkError? = null
    ) = initResponse(EmailsSummaryResponse::class.java, data ?: mock(), error)

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
