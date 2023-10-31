package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert
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
import org.wordpress.android.fluxc.network.AcceptHeaderStrategy
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload as BlazeCampaignsFetchedPayload

private val CONTENT_CONFIG_RESPONSE = ContentConfig(
    title = "Brand new post - do not approve",
    imageUrl = "https://imageurl"
)

private val CONTENT_CAMPAIGN_STATS = CampaignStats(
    impressionsTotal = 1,
    clicksTotal = 1
)

private val CAMPAIGN_RESPONSE = Campaign(
    campaignId = 1,
    createdAt = "2023-06-02T00:00:00.000Z",
    endDate = "2023-06-02T00:00:00.000Z",
    budgetCents = 1000,
    contentImage = "undefined",
    uiStatus = "rejected",
    contentConfig = CONTENT_CONFIG_RESPONSE,
    campaignStats = CONTENT_CAMPAIGN_STATS
)

private val BLAZE_CAMPAIGNS_RESPONSE = BlazeCampaignsResponse(
    campaigns = listOf(CAMPAIGN_RESPONSE),
    page = 1,
    totalItems = 1,
    totalPages = 1
)

@RunWith(MockitoJUnitRunner::class)
class BlazeCampaignsRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var acceptHeaderStrategy: AcceptHeaderStrategy.JsonAcceptHeader

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BlazeCampaignsRestClient

    private val siteId: Long = 12

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BlazeCampaignsRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent,
            acceptHeaderStrategy
        )
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `when blaze campaigns are requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        val response = getResponseFromJsonString(json)
        initFetchBlazeCampaigns(data = response)

        restClient.fetchBlazeCampaigns(site)

        assertEquals(
            urlCaptor.firstValue,
            "${API_SITE_PATH}/${site.siteId}/$API_AUTH_BLAZE_CAMPAIGNS_PATH/${site.siteId}"
        )
    }

    @Test
    fun `given success call, when blaze campaigns is requested, then correct response is returned`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
            initFetchBlazeCampaigns(data = getResponseFromJsonString(json))

            val result = restClient.fetchBlazeCampaigns(site)
            assertSuccess(successResponse, result)
        }

    @Test
    fun `given timeout, when blaze campaigns is requested, then return timeout error`() = test {
        initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

        val result = restClient.fetchBlazeCampaigns(site)

        assertError(BlazeCampaignsErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when blaze campaigns is requested, then return api error`() = test {
        initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val result = restClient.fetchBlazeCampaigns(site)

        assertError(BlazeCampaignsErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when blaze campaigns is requested, then return invalid response error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

            val result = restClient.fetchBlazeCampaigns(site)

            assertError(BlazeCampaignsErrorType.INVALID_RESPONSE, result)
        }

    @Test
    fun `given not authenticated, when blaze campaigns is requested, then return auth required error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(NOT_AUTHENTICATED)))

            val result = restClient.fetchBlazeCampaigns(site)

            assertError(BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED, result)
        }

    @Test
    fun `given unknown error, when blaze campaigns is requested, then return generic error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

            val result = restClient.fetchBlazeCampaigns(site)

            assertError(BlazeCampaignsErrorType.GENERIC_ERROR, result)
        }

    private suspend fun initFetchBlazeCampaigns(
        data: BlazeCampaignsResponse? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val nonNullData = data ?: mock()
        val response = if (error != null) {
            Response.Error(error)
        } else {
            Response.Success(nonNullData)
        }

        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(BlazeCampaignsResponse::class.java),
                eq(false),
                any(),
                eq(false)
            )
        ).thenReturn(response)
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: BlazeCampaignsResponse,
        actual: BlazeCampaignsFetchedPayload<BlazeCampaignsResponse>
    ) {
        with(actual) {
            Assert.assertFalse(isError)
            Assert.assertEquals(expected.page, actual.response?.page)
            Assert.assertEquals(expected.totalItems, actual.response?.totalItems)
            Assert.assertEquals(expected.totalPages, actual.response?.totalPages)
            Assert.assertEquals(expected.campaigns, actual.response?.campaigns)
        }
    }

    private fun assertError(
        expected: BlazeCampaignsErrorType,
        actual: BlazeCampaignsFetchedPayload<BlazeCampaignsResponse>
    ) {
        with(actual) {
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    private fun getResponseFromJsonString(json: String): BlazeCampaignsResponse {
        val responseType = object : TypeToken<BlazeCampaignsResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as BlazeCampaignsResponse
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_AUTH_BLAZE_CAMPAIGNS_PATH = "wordads/dsp/api/v1/search/campaigns/site"
        private const val SUCCESS_JSON = "wp/blaze/blaze-campaigns.json"
    }
}
