package org.wordpress.android.fluxc.network.rest.wpcom.qrcode

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
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
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeAuthenticateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeValidateResponse
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class QrcodeRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: QrcodeRestClient

    private val validateSuccess = QrcodeValidateResponse(
        browser = "Chrome",
        location = "Secaucus, New Jersey",
        success = true
    )

    private val authenticateSuccess = QrcodeAuthenticateResponse(
        authenticated = true
    )

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = QrcodeRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    // VALIDATE TESTS
    @Test
    fun `when validate is requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, VALIDATE_SUCCESS_JSON)
        val response = getValidateResponseFromJsonString(json)
        initPostValidate(data = response)

        restClient.validate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertEquals(urlCaptor.firstValue,
            "$API_BASE_PATH/$API_AUTH_QRCODE_VALIDATE_PATH")
    }

    @Test
    fun `given success, when validate is triggered, then validate response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, VALIDATE_SUCCESS_JSON)
        val response = getValidateResponseFromJsonString(json)
        initPostValidate(data = response)

        val result = restClient.validate(TOKEN_PARAM, DATA_PARAM)

        assertValidateSuccess(validateSuccess, result)
    }

    @Test
    fun `given data invalid, when validate is triggered, then error response is returned`() = test {
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.DATA_INVALID.name.lowercase()
        }
        initPostValidate(error = error)

        val result = restClient.validate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.DATA_INVALID, result)
    }

    @Test
    fun `given rest invalid param, when validate is triggered, then error response is returned`() = test {
        // {"code":"rest_invalid_param","message":"Data is invalid (1) invalid base64 string","data":{"status":400}}
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.REST_INVALID_PARAM.name.lowercase()
        }
        initPostValidate(error = error)

        val result = restClient.validate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.REST_INVALID_PARAM, result)
    }

    @Test
    fun `given not authorized, when validate is triggered, then error response is returned`() = test {
        // {"code":"not_authorized","message":"Did not authorize the user","data":{"status":400}}
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.NOT_AUTHORIZED.name.lowercase()
        }
        initPostValidate(error = error)

        val result = restClient.validate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.NOT_AUTHORIZED, result)
    }

    @Test
    fun `given network error, when validate is triggered, then error response is returned`() = test {
        initPostValidate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

        val result = restClient.validate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.API_ERROR, result)
    }

    @Test
    fun `given timeout error, when validate is triggered, then error response is returned`() = test {
        initPostValidate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT)))

        val result = restClient.validate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.TIMEOUT, result)
    }

    @Test
    fun `given invalid response, when validate is triggered, then error response is returned`() = test {
        initPostValidate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE)))

        val result = restClient.validate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given not authenticated, when validate is triggered, then error response is returned`() = test {
        initPostValidate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED)))

        val result = restClient.validate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.AUTHORIZATION_REQUIRED, result)
    }

    // AUTHENTICATE TESTS
    @Test
    fun `when authenticate is requested, then the correct url is built`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, AUTHENTICATE_SUCCESS_JSON)
        val response = getAuthenticateResponseFromJsonString(json)
        initPostAuthenticate(data = response)

        restClient.authenticate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertEquals(urlCaptor.firstValue,
            "$API_BASE_PATH/$API_AUTH_QRCODE_AUTHENTICATE_PATH")
    }

    @Test
    fun `given success, when authenticate is triggered, then validate response is returned`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, AUTHENTICATE_SUCCESS_JSON)
        val response = getAuthenticateResponseFromJsonString(json)
        initPostAuthenticate(data = response)

        val result = restClient.authenticate(TOKEN_PARAM, DATA_PARAM)

        assertAuthenticateSuccess(authenticateSuccess, result)
    }

    @Test
    fun `given data invalid, when authenticate is triggered, then error response is returned`() = test {
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.DATA_INVALID.name.lowercase()
        }
        initPostAuthenticate(error = error)

        val result = restClient.authenticate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.DATA_INVALID, result)
    }

    @Test
    fun `given rest invalid param, when authenticate is triggered, then error response is returned`() = test {
        // {"code":"rest_invalid_param","message":"Data is invalid (1) invalid base64 string","data":{"status":400}}
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.REST_INVALID_PARAM.name.lowercase()
        }
        initPostAuthenticate(error = error)

        val result = restClient.authenticate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.REST_INVALID_PARAM, result)
    }

    @Test
    fun `given not authorized, when authenticate is triggered, then error response is returned`() = test {
        // {"code":"not_authorized","message":"Did not authorize the user","data":{"status":400}}
        val error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.UNKNOWN)).apply {
            apiError = QrcodeErrorType.NOT_AUTHORIZED.name.lowercase()
        }
        initPostAuthenticate(error = error)

        val result = restClient.authenticate(TOKEN_PARAM, DATA_PARAM)

        assertTrue(result.isError)
        assertError(QrcodeErrorType.NOT_AUTHORIZED, result)
    }

    @Test
    fun `given network error, when authenticate is triggered, then error response is returned`() = test {
        initPostAuthenticate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR)))

        val result = restClient.authenticate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.API_ERROR, result)
    }

    @Test
    fun `given timeout error, when authenticate is triggered, then error response is returned`() = test {
        initPostAuthenticate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.TIMEOUT)))

        val result = restClient.authenticate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.TIMEOUT, result)
    }

    @Test
    fun `given invalid response, when authenticate is triggered, then error response is returned`() = test {
        initPostAuthenticate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.INVALID_RESPONSE)))

        val result = restClient.authenticate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.INVALID_RESPONSE, result)
    }

    @Test
    fun `given not authenticated, when authenticate is triggered, then error response is returned`() = test {
        initPostAuthenticate(error = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NOT_AUTHENTICATED)))

        val result = restClient.authenticate(token = TOKEN_PARAM, data = DATA_PARAM)

        assertError(QrcodeErrorType.AUTHORIZATION_REQUIRED, result)
    }

    private suspend fun initPostValidate(
        data: QrcodeValidateResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<QrcodeValidateResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Response.Success(nonNullData)

        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                restClient = eq(restClient),
                url = urlCaptor.capture(),
                params = paramsCaptor.capture(),
                body = anyOrNull(),
                clazz = eq(QrcodeValidateResponse::class.java),
                retryPolicy = isNull()
            )
        ).thenReturn(response)

        return response
    }

    private suspend fun initPostAuthenticate(
        data: QrcodeAuthenticateResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<QrcodeAuthenticateResponse> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Response.Error(error) else Response.Success(nonNullData)

        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                restClient = eq(restClient),
                url = urlCaptor.capture(),
                params = paramsCaptor.capture(),
                body = anyOrNull(),
                clazz = eq(QrcodeAuthenticateResponse::class.java),
                retryPolicy = isNull()
            )
        ).thenReturn(response)

        return response
    }

    private fun assertValidateSuccess(
        expected: QrcodeValidateResponse,
        actual: QrcodePayload<QrcodeValidateResponse>
    ) {
        with(actual) {
            assertFalse(isError)
            assertEquals(QrcodePayload(expected), this)
        }
    }

    private fun assertAuthenticateSuccess(
        expected: QrcodeAuthenticateResponse,
        actual: QrcodePayload<QrcodeAuthenticateResponse>
    ) {
        with(actual) {
            assertFalse(isError)
            assertEquals(QrcodePayload(expected), this)
        }
    }

    private fun <T> assertError(
        expected: QrcodeErrorType,
        actual: QrcodePayload<T>
    ) {
        with(actual) {
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    private fun getValidateResponseFromJsonString(json: String): QrcodeValidateResponse {
        val responseType = object : TypeToken<QrcodeValidateResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as QrcodeValidateResponse
    }

    private fun getAuthenticateResponseFromJsonString(json: String): QrcodeAuthenticateResponse {
        val responseType = object : TypeToken<QrcodeAuthenticateResponse>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as QrcodeAuthenticateResponse
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_AUTH_QRCODE_VALIDATE_PATH = "auth/qr-code/validate/"
        private const val API_AUTH_QRCODE_AUTHENTICATE_PATH = "auth/qr-code/authenticate/"

        private const val TOKEN_PARAM = "token_param"
        private const val DATA_PARAM = "data_param"

        private const val VALIDATE_SUCCESS_JSON = "wp/qrcode/validate-success.json"
        private const val AUTHENTICATE_SUCCESS_JSON = "wp/qrcode/authenticate-success.json"
    }
}
