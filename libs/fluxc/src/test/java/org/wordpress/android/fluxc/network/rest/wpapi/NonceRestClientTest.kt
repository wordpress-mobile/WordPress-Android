package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Header
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NonceRestClientTest {
    private val wpApiEncodedRequestBuilder: WPAPIEncodedBodyRequestBuilder = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var subject: NonceRestClient
    private val time = 123456L

    private val site = SiteModel().apply {
        url = "asiteurl.com"
        username = "a_username"
        password = "a_password"
    }
    private val nonceRequestUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

    @Before
    fun setUp() {
        subject = NonceRestClient(wpApiEncodedRequestBuilder, currentTimeProvider, dispatcher, requestQueue, userAgent)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(time))
    }

    @Test
    fun `successful nonce request`() = test {
        val redirectResponse = WPAPIResponse.Error<String>(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            301,
                            byteArrayOf(),
                            false,
                            System.currentTimeMillis(),
                            listOf(Header("Location", nonceRequestUrl))
                        )
                    )
                ),
                null
            )
        )
        val expectedNonce = "1expectedNONCE"
        givenLoginResponse(redirectResponse)
        givenNonceRequestResponse(WPAPIResponse.Success(expectedNonce))

        val actual = subject.requestNonce(site)

        TestCase.assertEquals(Nonce.Available(expectedNonce, site.username), actual)
    }

    @Test
    fun `invalid credentials returns correct error message`() = test {
        @Suppress("MaxLineLength")
        val loginResponse = WPAPIResponse.Success(
            """
            <html>
              <head>
                    <div id="login_error">
                        <strong>Error:</strong> The password you entered for the username <strong>demo</strong> is incorrect. <a href="link/">Lost your password?</a><br>
                    </div>
              </head>
            </html>
        """.trimIndent()
        )
        givenLoginResponse(loginResponse)

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.NOT_AUTHENTICATED, actual.type)
        assertEquals("Error: The password you entered for the username demo is incorrect.", actual.errorMessage)
    }

    @Test
    fun `invalid nonce of '0' returns FailedRequest`() = test {
        val redirectUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

        val redirectResponse = WPAPIResponse.Error<String>(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            301,
                            byteArrayOf(),
                            false,
                            System.currentTimeMillis(),
                            listOf(Header("Location", redirectUrl))
                        )
                    )
                ),
                null
            )
        )

        val invalidNonce = "0"
        val response = WPAPIResponse.Success(invalidNonce)
        givenLoginResponse(redirectResponse)
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, redirectUrl))
            .thenReturn(response)

        val actual = subject.requestNonce(site)
        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(time, actual.timeOfResponse)
        assertEquals(Nonce.CookieNonceErrorType.INVALID_NONCE, actual.type)
    }

    @Test
    fun `failed nonce request return FailedRequest`() = test {
        val baseNetworkError = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(400, byteArrayOf(), false, System.currentTimeMillis(), listOf())
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(baseNetworkError))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(time, actual.timeOfResponse)
        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, actual.type)
        assertEquals(baseNetworkError, actual.networkError)
    }

    @Test
    fun `failed nonce request with connection error returns Unknown`() = test {
        val baseNetworkError = mock<WPAPINetworkError>()
        baseNetworkError.volleyError = NoConnectionError()
        givenLoginResponse(WPAPIResponse.Error(baseNetworkError))

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(Nonce.Unknown(site.username), actual)
    }

    @Test
    fun `custom login URL returns correct error type`() = test {
        val error = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(
                        404,
                        byteArrayOf(),
                        false,
                        System.currentTimeMillis(),
                        listOf()
                    )
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(error))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, actual.type)
    }

    @Test
    fun `custom admin URL returns correct error type`() = test {
        val redirectResponse = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(
                        301,
                        byteArrayOf(),
                        false,
                        System.currentTimeMillis(),
                        listOf(Header("Location", nonceRequestUrl))
                    )
                )
            ),
            null
        )
        val nonceError = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(404, byteArrayOf(), false, System.currentTimeMillis(), listOf())
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(redirectResponse))
        givenNonceRequestResponse(WPAPIResponse.Error(nonceError))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, actual.type)
    }

    private suspend fun givenLoginResponse(response: WPAPIResponse<String>) {
        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to nonceRequestUrl
        )

        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(response)
    }

    private suspend fun givenNonceRequestResponse(response: WPAPIResponse<String>) {
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, nonceRequestUrl))
            .thenReturn(response)
    }
}
