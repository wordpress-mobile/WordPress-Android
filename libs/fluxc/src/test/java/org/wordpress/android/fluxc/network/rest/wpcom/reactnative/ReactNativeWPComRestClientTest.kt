package org.wordpress.android.fluxc.network.rest.wpcom.reactnative

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import junit.framework.AssertionFailedError
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    val body = mapOf("b_key" to "b_value")

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
    fun `GET request handles successful response`() = test {
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
        verifyGETRequest(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `GET request handles failure response`() = test {
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
        verifyGETRequest(successHandler, errorHandler, mockedRestCallResponse, expected)
    }

    @Test
    fun `POST request handles successful response`() = test {
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
        verifyPOSTRequest(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `POST request handles failure response`() = test {
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
        verifyPOSTRequest(successHandler, errorHandler, mockedRestCallResponse, expected)
    }

    private suspend fun verifyGETRequest(
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
                true,
        )
        ).thenReturn(expectedRestCallResponse)

        val actual = subject.getRequest(url, params, successHandler, errorHandler)
        assertEquals(expected, actual)
    }

    private suspend fun verifyPOSTRequest(
        successHandler: (JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        expectedRestCallResponse: WPComGsonRequestBuilder.Response<JsonElement>,
        expected: ReactNativeFetchResponse
    ) {
        whenever(wpComGsonRequestBuilder.syncPostRequest(
            subject,
            url,
            params,
            body,
            JsonElement::class.java)
        ).thenReturn(expectedRestCallResponse)

        val actual = subject.postRequest(url, params, body, successHandler, errorHandler)
        assertEquals(expected, actual)
    }
}
