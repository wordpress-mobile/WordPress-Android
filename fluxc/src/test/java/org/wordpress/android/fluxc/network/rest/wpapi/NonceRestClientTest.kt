package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class NonceRestClientTest {
    @Mock lateinit var wpApiEncodedRequestBuilder: WPAPIEncodedBodyRequestBuilder
    @Mock lateinit var currentTimeProvider: CurrentTimeProvider
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var requestQueue: RequestQueue
    @Mock lateinit var userAgent: UserAgent

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

        val body = mapOf(
                "log" to site.username,
                "pwd" to site.password,
                "redirect_to" to "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"
        )

        val expectedNonce = "1expectedNONCE"
        val response = Success(expectedNonce)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
                .thenReturn(response)

        val actual = subject.requestNonce(site)

        TestCase.assertEquals(Available(expectedNonce), actual)
    }

    @Test
    fun `invalid nonce of '0' returns FailedRequest`() = test {
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

        val invalidNonce = "0"
        val response = Success(invalidNonce)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
                .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(FailedRequest(time), actual)
    }

    @Test
    fun `failed nonce request reuturn FailedRequest`() = test {
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

        val baseNetworkError = mock<BaseNetworkError>()
        baseNetworkError.message = "an_error_message"
        val response = Error<String>(baseNetworkError)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
                .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(FailedRequest(time), actual)
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

        val baseNetworkError = mock<BaseNetworkError>()
        baseNetworkError.volleyError = NoConnectionError()
        val response = Error<String>(baseNetworkError)
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
                .thenReturn(response)

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(Unknown, actual)
    }
}
