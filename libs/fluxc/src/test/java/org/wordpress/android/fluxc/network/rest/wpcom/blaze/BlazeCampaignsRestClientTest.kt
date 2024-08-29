package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient.Companion.DEFAULT_PER_PAGE
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class BlazeCampaignsRestClientTest {
    private val wpComNetwork: WPComNetwork = mock()

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BlazeCampaignsRestClient

    private val siteId: Long = 12

    private val successResponse = BLAZE_CAMPAIGNS_RESPONSE

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BlazeCampaignsRestClient(wpComNetwork)
    }

    @Test
    fun `when blaze campaigns are requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        val response = getResponseFromJsonString(json)
        initFetchBlazeCampaigns(data = response)

        restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

        assertEquals(
            urlCaptor.firstValue,
            WPCOMV2.sites.site(siteId).wordads.dsp.api.v1_1.campaigns.url
        )
    }

    @Test
    fun `given success call, when blaze campaigns is requested, then correct response is returned`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
            initFetchBlazeCampaigns(data = getResponseFromJsonString(json))

            val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)
            assertSuccess(successResponse, result)
        }

    @Test
    fun `given timeout, when blaze campaigns is requested, then return timeout error`() = test {
        initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

        val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

        assertError(BlazeCampaignsErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when blaze campaigns is requested, then return api error`() = test {
        initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

        assertError(BlazeCampaignsErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when blaze campaigns is requested, then return invalid response error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

            val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

            assertError(BlazeCampaignsErrorType.INVALID_RESPONSE, result)
        }

    @Test
    fun `given not authenticated, when blaze campaigns is requested, then return auth required error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(NOT_AUTHENTICATED)))

            val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

            assertError(BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED, result)
        }

    @Test
    fun `given unknown error, when blaze campaigns is requested, then return generic error`() =
        test {
            initFetchBlazeCampaigns(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

            val result = restClient.fetchBlazeCampaigns(siteId, SKIP, DEFAULT_PER_PAGE, DEFAULT_LOCALE)

            assertError(BlazeCampaignsErrorType.GENERIC_ERROR, result)
        }

    private suspend fun initFetchBlazeCampaigns(
        data: BlazeCampaignListResponse? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val nonNullData = data ?: mock()
        val response = if (error != null) {
            Response.Error(error)
        } else {
            Response.Success(nonNullData)
        }

        whenever(
            wpComNetwork.executeGetGsonRequest(
                url = urlCaptor.capture(),
                clazz = eq(BlazeCampaignListResponse::class.java),
                params = paramsCaptor.capture(),
                enableCaching = eq(false),
                cacheTimeToLive = any(),
                forced = eq(false)
            )
        ).thenReturn(response)
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: BlazeCampaignListResponse,
        actual: BlazeCampaignsFetchedPayload<BlazeCampaignListResponse>
    ) {
        with(actual) {
            Assert.assertFalse(isError)
            Assert.assertEquals(expected.skipped, actual.response?.skipped)
            Assert.assertEquals(expected.totalCount, actual.response?.totalCount)
            Assert.assertEquals(expected.campaigns, actual.response?.campaigns)
        }
    }

    private fun assertError(
        expected: BlazeCampaignsErrorType,
        actual: BlazeCampaignsFetchedPayload<BlazeCampaignListResponse>
    ) {
        with(actual) {
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    private fun getResponseFromJsonString(json: String): BlazeCampaignListResponse {
        val responseType = object : TypeToken<BlazeCampaignListResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as BlazeCampaignListResponse
    }

    private companion object {
        const val SUCCESS_JSON = "wp/blaze/blaze-campaigns.json"
        const val CAMPAIGN_ID = "1234"
        const val TITLE = "title"
        const val IMAGE_URL = "imageUrl"
        const val CREATED_AT = "2023-06-02T00:00:00.000Z"
        const val DURATION_IN_DAYS = 10
        const val UI_STATUS = "rejected"
        const val IMPRESSIONS = 0L
        const val CLICKS = 0L
        const val TOTAL_BUDGET = 100.0
        const val SPENT_BUDGET = 0.0

        const val SKIP = 0
        const val TOTAL_ITEMS = 1
        const val DEFAULT_LOCALE = "en"

        val CAMPAIGN_IMAGE = CampaignImage(
            height = 100f,
            width = 100f,
            mimeType = "image/jpeg",
            url = IMAGE_URL
        )

        val CAMPAIGN_RESPONSE = BlazeCampaign(
            id = CAMPAIGN_ID,
            image = CAMPAIGN_IMAGE,
            targetUrl = "https://example.com",
            textSnippet = TITLE,
            siteName = "siteName",
            clicks = CLICKS,
            impressions = IMPRESSIONS,
            spentBudget = SPENT_BUDGET,
            totalBudget = TOTAL_BUDGET,
            durationDays = DURATION_IN_DAYS,
            startTime = CREATED_AT,
            targetUrn = "urn:wpcom:post:199247490:9",
            status = UI_STATUS,
            isEvergreen = false
        )

        val BLAZE_CAMPAIGNS_RESPONSE = BlazeCampaignListResponse(
            campaigns = listOf(CAMPAIGN_RESPONSE),
            skipped = SKIP,
            totalCount = TOTAL_ITEMS,
        )
    }
}
