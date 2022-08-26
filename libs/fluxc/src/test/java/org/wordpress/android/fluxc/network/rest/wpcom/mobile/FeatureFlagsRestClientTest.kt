package org.wordpress.android.fluxc.network.rest.wpcom.mobile

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
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
class FeatureFlagsRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: FeatureFlagsRestClient
    
    private val successResponse = mapOf("flag-1" to true, "flag-2" to false)

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = FeatureFlagsRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `when feature flags are requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        val response = getResponseFromJsonString(json)
        initFetchFeatureFlags(data = response)

        restClient.fetchFeatureFlags(
                buildNumber = BUILD_NUMBER_PARAM,
                deviceId = DEVICE_ID_PARAM,
                identifier = IDENTIFIER_PARAM,
                marketingVersion = MARKETING_VERSION_PARAM,
                platform = PLATFORM_PARAM
            )

        assertEquals(urlCaptor.firstValue,
            "${API_BASE_PATH}/${API_AUTH_MOBILE_FEATURE_FLAG_PATH}")
    }

    @Test
    fun `given success call, when f-flags are requested, then correct response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, SUCCESS_JSON)
        initFetchFeatureFlags(data = getResponseFromJsonString(json))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertSuccess(successResponse, result)
    }

    @Test
    fun `given timeout, when f-flags are requested, then return timeout error`() = test {
        initFetchFeatureFlags(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertError(FeatureFlagsErrorType.TIMEOUT, result)
    }

    @Test
    fun `given network error, when f-flags are requested, then return api error`() = test {
        initFetchFeatureFlags(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertError(FeatureFlagsErrorType.API_ERROR, result)
    }

    @Test
    fun `given invalid response, when f-flags are requested, then return invalid response error`() = test {
        initFetchFeatureFlags(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertError(FeatureFlagsErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given not authenticated, when f-flags are requested, then return auth required error`() = test {
        initFetchFeatureFlags(error = WPComGsonNetworkError(BaseNetworkError(NOT_AUTHENTICATED)))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertError(FeatureFlagsErrorType.AUTH_ERROR, result)
    }

    @Test
    fun `given unknown error, when f-flags are requested, then return generic error`() = test {
        initFetchFeatureFlags(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

        val result = restClient.fetchFeatureFlags(
            buildNumber = BUILD_NUMBER_PARAM,
            deviceId = DEVICE_ID_PARAM,
            identifier = IDENTIFIER_PARAM,
            marketingVersion = MARKETING_VERSION_PARAM,
            platform = PLATFORM_PARAM)

        assertError(FeatureFlagsErrorType.GENERIC_ERROR, result)
    }


    private suspend fun initFetchFeatureFlags(
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
        actual: FeatureFlagsFetchedPayload
    ) {
        with(actual) {
            Assert.assertFalse(isError)
            Assert.assertEquals(FeatureFlagsFetchedPayload(expected), this)
        }
    }

    private fun assertError(
        expected: FeatureFlagsErrorType,
        actual: FeatureFlagsFetchedPayload
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
        private const val API_AUTH_MOBILE_FEATURE_FLAG_PATH = "mobile/feature-flags/"

        private const val BUILD_NUMBER_PARAM = "build_number_param"
        private const val DEVICE_ID_PARAM = "device_id_param"
        private const val IDENTIFIER_PARAM = "identifier_param"
        private const val MARKETING_VERSION_PARAM = "marketing_version_param"
        private const val PLATFORM_PARAM = "platform_param"

        private const val SUCCESS_JSON = "wp/mobile/feature-flags-success.json"
    }
}
