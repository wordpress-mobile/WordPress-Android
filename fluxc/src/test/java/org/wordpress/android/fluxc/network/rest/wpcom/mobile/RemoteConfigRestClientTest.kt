package org.wordpress.android.fluxc.network.rest.wpcom.mobile

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
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
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
class RemoteConfigRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: RemoteConfigClient
    
    private val successResponse = mapOf("jp-deadline" to "2022-10-10")

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = RemoteConfigClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `when remote configs are requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        val response = getResponseFromJsonString(json)
        initFetchRemoteConfig(data = response)

        restClient.fetchRemoteConfig()

        assertEquals(urlCaptor.firstValue,
            "${API_BASE_PATH}/${API_MOBILE_REMOTE_CONFIG_PATH}")
    }

    @Test
    fun `given success call, when remote-config are requested, then correct response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        initFetchRemoteConfig(data = getResponseFromJsonString(json))

        val result = restClient.fetchRemoteConfig()

        assertSuccess(successResponse, result)
    }

    @Test
    fun `given timeout, when remote-config are requested, then return timeout error`() = test {
        initFetchRemoteConfig(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

        val result = restClient.fetchRemoteConfig()

        assertError(RemoteConfigErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when remote-config are requested, then return api error`() = test {
        initFetchRemoteConfig(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val result = restClient.fetchRemoteConfig()

        assertError(RemoteConfigErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when remote-config are requested, then return invalid response error`() = test {
        initFetchRemoteConfig(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

        val result = restClient.fetchRemoteConfig()

        assertError(RemoteConfigErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given unknown error, when remote-config are requested, then return generic error`() = test {
        initFetchRemoteConfig(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

        val result = restClient.fetchRemoteConfig()

        assertError(RemoteConfigErrorType.GENERIC_ERROR, result)
    }


    private suspend fun initFetchRemoteConfig(
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
        expected: Map<String, String>,
        actual: RemoteConfigFetchedPayload
    ) {
        with(actual) {
            Assert.assertFalse(isError)
            Assert.assertEquals(RemoteConfigFetchedPayload(expected), this)
        }
    }

    private fun assertError(
        expected: RemoteConfigErrorType,
        actual: RemoteConfigFetchedPayload
    ) {
        with(actual) {
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    private fun getResponseFromJsonString(json: String): Map<String, String> {
        val responseType = object : TypeToken<Map<*, *>>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as Map<String, String>
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_MOBILE_REMOTE_CONFIG_PATH = "mobile/remote-config/"

        private const val SUCCESS_JSON = "wp/mobile/remote-config-success.json"
    }
}
