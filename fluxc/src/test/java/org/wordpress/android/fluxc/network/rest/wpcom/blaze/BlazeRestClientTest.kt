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

@RunWith(MockitoJUnitRunner::class)
class BlazeRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BlazeRestClient

    private val siteId: Long = 12

    private val successResponse = mapOf("approved" to true)

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BlazeRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
        whenever(site.siteId).thenReturn(siteId)
    }


    @Test
    fun `when blaze status is requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        val response = getResponseFromJsonString(json)
        initFetchBlazeStatus(data = response)

        restClient.fetchBlazeStatus(site)

        assertEquals(urlCaptor.firstValue,
            "${API_SITE_PATH}/${site.siteId}/$API_AUTH_BLAZE_STATUS_PATH")
    }

    @Test
    fun `given success call, when blaze status is requested, then correct response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        initFetchBlazeStatus(data = getResponseFromJsonString(json))

        val result = restClient.fetchBlazeStatus(site)
        assertSuccess(successResponse, result)
    }

    @Test
    fun `given timeout, when blaze status is requested, then return timeout error`() = test {
        initFetchBlazeStatus(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

        val result = restClient.fetchBlazeStatus(site)

        assertError(BlazeStatusErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when blaze status is requested, then return api error`() = test {
        initFetchBlazeStatus(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val result = restClient.fetchBlazeStatus(site)

        assertError(BlazeStatusErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when blaze status is requested, then return invalid response error`() = test {
        initFetchBlazeStatus(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

        val result = restClient.fetchBlazeStatus(site)

        assertError(BlazeStatusErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given not authenticated, when blaze status is requested, then return auth required error`() = test {
        initFetchBlazeStatus(error = WPComGsonNetworkError(BaseNetworkError(NOT_AUTHENTICATED)))

        val result = restClient.fetchBlazeStatus(site)

        assertError(BlazeStatusErrorType.AUTHORIZATION_REQUIRED, result)
    }

    @Test
    fun `given unknown error, when blaze status is requested, then return generic error`() = test {
        initFetchBlazeStatus(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

        val result = restClient.fetchBlazeStatus(site)

        assertError(BlazeStatusErrorType.GENERIC_ERROR, result)
    }


    private suspend fun initFetchBlazeStatus(
        data: Map<*, *>? = null,
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
                eq(Map::class.java),
                eq(false),
                any(),
                eq(false)
            )
        ).thenReturn(response)
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: Map<String, Boolean>,
        actual: BlazeStatusFetchedPayload
    ) {
        with(actual) {
            Assert.assertFalse(isError)
            Assert.assertEquals(BlazeStatusFetchedPayload(siteId, expected), this)
        }
    }

    private fun assertError(
        expected: BlazeStatusErrorType,
        actual: BlazeStatusFetchedPayload
    ) {
        with(actual) {
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    private fun getResponseFromJsonString(json: String): Map<String, Boolean> {
        val responseType = object : TypeToken<Map<*, *>>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as Map<String, Boolean>
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_AUTH_BLAZE_STATUS_PATH = "blaze/status/"

        private const val SUCCESS_JSON = "wp/blaze/blaze-status.json"
    }
}
