package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.AssertionFailedError
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIEncodedBodyRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Unknown
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.test

class ReactNativeWPAPIRestClientTest {
    private val wpApiGsonRequestBuilder = mock<WPAPIGsonRequestBuilder>()
    private val wpApiEncodedRequestBuilder = mock<WPAPIEncodedBodyRequestBuilder>()
    private val dispatcher = mock<Dispatcher>()
    private val requestQueue = mock<RequestQueue>()
    private val userAgent = mock<UserAgent>()

    private val url = "a_url"
    private val params = mapOf("a_key" to "a_value")
    private val time = 123456L

    private lateinit var subject: ReactNativeWPAPIRestClient

    @Before
    fun setUp() {
        subject = ReactNativeWPAPIRestClient(
                wpApiGsonRequestBuilder,
                wpApiEncodedRequestBuilder,
                dispatcher,
                requestQueue,
                userAgent,
                { time }
        )
    }

    @Test
    fun `fetch handles successful response`() = test {
        val errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse = { _ ->
            throw AssertionFailedError("errorHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedJson = mock<JsonElement>()
        val successHandler = { data: JsonElement? ->
            if (data != expectedJson) fail("expected data was not passed to successHandler")
            expected
        }

        val expectedRestCallResponse = Success(expectedJson)
        verifyRestApi(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `fetch handles failure response`() = test {
        val successHandler = { _: JsonElement? ->
            throw AssertionFailedError("successHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedBaseNetworkError = mock<BaseNetworkError>()
        val errorHandler = { error: BaseNetworkError ->
            if (error != expectedBaseNetworkError) fail("expected error was not passed to errorHandler")
            expected
        }

        val mockedRestCallResponse = Error<JsonElement>(expectedBaseNetworkError)
        verifyRestApi(successHandler, errorHandler, mockedRestCallResponse, expected)
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

        assertEquals(Available(expectedNonce), actual)
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
        assertEquals(FailedRequest(time), actual)
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
        assertEquals(FailedRequest(time), actual)
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
        assertEquals(Unknown, actual)
    }

    private suspend fun verifyRestApi(
        successHandler: (JsonElement?) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        expectedRestCallResponse: WPAPIResponse<JsonElement>,
        expected: ReactNativeFetchResponse
    ) {
        whenever(wpApiGsonRequestBuilder.syncGetRequest(
                subject,
                url,
                params,
                emptyMap(),
                JsonElement::class.java,
                true)
        ).thenReturn(expectedRestCallResponse)

        val actual = subject.fetch(url, params, successHandler, errorHandler)
        assertEquals(expected, actual)
    }
}
