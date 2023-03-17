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
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

class NonceRestClientTest {
    private val wpApiEncodedRequestBuilder: WPAPIEncodedBodyRequestBuilder = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var subject: NonceRestClient
    private val time = 123456L

    @Before
    fun setUp() {
        subject = NonceRestClient(wpApiEncodedRequestBuilder, currentTimeProvider, dispatcher, requestQueue, userAgent)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(time))
    }

    @Test
    fun `successful nonce request`() = test {
        val site = SiteModel().apply {
            url = "asiteurl.com"
            username = "a_username"
            password = "a_password"
        }
        val redirectUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to redirectUrl
        )

        val redirectResponse = Error<String>(
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
        val expectedNonce = "1expectedNONCE"
        val successResponse = Success(expectedNonce)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(redirectResponse)
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, redirectUrl))
            .thenReturn(successResponse)

        val actual = subject.requestNonce(site)

        TestCase.assertEquals(Available(expectedNonce, site.username), actual)
    }

    @Test
    fun `invalid nonce of '0' returns FailedRequest`() = test {
        val site = SiteModel().apply {
            url = "asiteurl.com"
            username = "a_username"
            password = "a_password"
        }
        val redirectUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to redirectUrl
        )

        val redirectResponse = Error<String>(
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
        val response = Success(invalidNonce)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(redirectResponse)
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, redirectUrl))
            .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(FailedRequest(time, site.username, Nonce.CookieNonceErrorType.INVALID_NONCE), actual)
    }

    @Test
    fun `failed nonce request return FailedRequest`() = test {
        val site = SiteModel().apply {
            url = "asiteurl.com"
            username = "a_username"
            password = "a_password"
        }

        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"
        )

        val baseNetworkError = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(400, byteArrayOf(), false, System.currentTimeMillis(), listOf())
                )
            )
        )
        val response = Error<String>(baseNetworkError)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(
            FailedRequest(
                time,
                site.username,
                Nonce.CookieNonceErrorType.GENERIC_ERROR,
                baseNetworkError,
                ""
            ),
            actual
        )
    }

    @Test
    fun `failed nonce request with connection error returns Unknown`() = test {
        val site = SiteModel().apply {
            url = "asiteurl.com"
            username = "a_username"
            password = "a_password"
        }

        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"
        )

        val baseNetworkError = mock<WPAPINetworkError>()
        baseNetworkError.volleyError = NoConnectionError()
        val response = Error<String>(baseNetworkError)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(Unknown(site.username), actual)
    }
}
