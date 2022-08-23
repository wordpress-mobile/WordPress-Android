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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
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
        initSuccess(data = response)

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

    private suspend fun initSuccess(
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
                eq(true)
            )
        ).thenReturn(response)
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
