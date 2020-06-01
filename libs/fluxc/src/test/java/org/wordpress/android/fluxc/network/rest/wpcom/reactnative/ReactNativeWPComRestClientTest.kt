package org.wordpress.android.fluxc.network.rest.wpcom.reactnative

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.AssertionFailedError
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.test

class ReactNativeWPComRestClientTest {
    private val wpComGsonRequestBuilder = mock<WPComGsonRequestBuilder>()
    private val context = mock<Context>()
    private val dispatcher = mock<Dispatcher>()
    private val requestQueue = mock<RequestQueue>()
    private val accessToken = mock<AccessToken>()
    private val userAgent = mock<UserAgent>()

    private val url = "a_url"
    val params = mapOf("a_key" to "a_value")

    private lateinit var subject: ReactNativeWPComRestClient

    @Before
    fun setUp() {
        subject = ReactNativeWPComRestClient(
                wpComGsonRequestBuilder,
                context,
                dispatcher,
                requestQueue,
                accessToken,
                userAgent)
    }

    @Test
    fun `fetch handles successful response`() = test {
        val errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse = { _ ->
            throw AssertionFailedError("errorHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedJson = mock<JsonElement>()
        val successHandler = { data: JsonElement ->
            if (data != expectedJson) fail("expected data was not passed to successHandler")
            expected
        }

        val expectedRestCallResponse = Success(expectedJson)
        verifyRestApi(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `fetch handles failure response`() = test {
        val successHandler = { _: JsonElement ->
            throw AssertionFailedError("successHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedBaseNetworkError = mock<WPComGsonNetworkError>()
        val errorHandler = { error: BaseNetworkError ->
            if (error != expectedBaseNetworkError) fail("expected error was not passed to errorHandler")
            expected
        }

        val mockedRestCallResponse = Error<JsonElement>(expectedBaseNetworkError)
        verifyRestApi(successHandler, errorHandler, mockedRestCallResponse, expected)
    }

    private suspend fun verifyRestApi(
        successHandler: (JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        expectedRestCallResponse: WPComGsonRequestBuilder.Response<JsonElement>,
        expected: ReactNativeFetchResponse
    ) {
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                subject,
                url,
                params,
                JsonElement::class.java,
                true)
        ).thenReturn(expectedRestCallResponse)

        val actual = subject.fetch(url, params, successHandler, errorHandler)
        assertEquals(expected, actual)
    }
}
