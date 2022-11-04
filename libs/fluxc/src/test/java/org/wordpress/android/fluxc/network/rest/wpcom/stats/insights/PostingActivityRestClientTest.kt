package org.wordpress.android.fluxc.network.rest.wpcom.stats.insights

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.POSTING_ACTIVITY_RESPONSE
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class PostingActivityRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var statsUtils: StatsUtils
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: PostingActivityRestClient
    private val siteId: Long = 12
    private val startDay = Day(2018, 1, 1)
    private val formattedStartDate = "2018-01-01"
    private val endDay = Day(2019, 1, 1)
    private val formattedEndDate = "2019-01-01"
    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = PostingActivityRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent,
                statsUtils
        )
        whenever(statsUtils.getFormattedDate(startDay)).thenReturn(formattedStartDate)
        whenever(statsUtils.getFormattedDate(endDay)).thenReturn(formattedEndDate)
    }

    @Test
    fun `returns posting activity`() = test {
        initResponse(POSTING_ACTIVITY_RESPONSE)

        val responseModel = restClient.fetchPostingActivity(site, startDay, endDay, forced = false)

        Assertions.assertThat(responseModel.response).isNotNull
        Assertions.assertThat(responseModel.response).isEqualTo(POSTING_ACTIVITY_RESPONSE)
        val url = "https://public-api.wordpress.com/rest/v1.1/sites/12/stats/streak/"
        Assertions.assertThat(urlCaptor.lastValue).isEqualTo(url)
        Assertions.assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf(
                        "endDate" to "2019-01-01",
                        "gmtOffset" to "0",
                        "max" to "3000",
                        "startDate" to "2018-01-01"
                )
        )
    }

    @Test
    fun `returns posting activity error response`() = test {
        val errorMessage = "message"
        initResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )

        val responseModel = restClient.fetchPostingActivity(site, startDay, endDay, forced = false)

        Assertions.assertThat(responseModel.error).isNotNull
        Assertions.assertThat(responseModel.error.type).isEqualTo(API_ERROR)
        Assertions.assertThat(responseModel.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initResponse(
        data: PostingActivityResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<PostingActivityResponse> {
        val response = if (error != null) Response.Error<PostingActivityResponse>(error) else Success(data ?: mock())
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(PostingActivityResponse::class.java),
                        eq(true),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
