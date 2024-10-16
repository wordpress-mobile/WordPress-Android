package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import junit.framework.AssertionFailedError
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.test

class ReactNativeWPAPIRestClientTest {
    private val wpApiGsonRequestBuilder = mock<WPAPIGsonRequestBuilder>()
    private val dispatcher = mock<Dispatcher>()
    private val requestQueue = mock<RequestQueue>()
    private val userAgent = mock<UserAgent>()

    private val url = "a_url"
    private val params = mapOf("a_key" to "a_value")
    private val body = mapOf("b_key" to "b_value")

    private lateinit var subject: ReactNativeWPAPIRestClient

    @Before
    fun setUp() {
        subject = ReactNativeWPAPIRestClient(
                wpApiGsonRequestBuilder,
                dispatcher,
                requestQueue,
                userAgent
        )
    }

    @Test
    fun `GET request handles successful response`() = test {
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
        verifyGETRequest(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `GET request handles failure response`() = test {
        val successHandler = { _: JsonElement? ->
            throw AssertionFailedError("successHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedBaseNetworkError = mock<WPAPINetworkError>()
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
        val successHandler = { data: JsonElement? ->
            if (data != expectedJson) fail("expected data was not passed to successHandler")
            expected
        }

        val expectedRestCallResponse = Success(expectedJson)
        verifyPOSTRequest(successHandler, errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `POST request handles failure response`() = test {
        val successHandler = { _: JsonElement? ->
            throw AssertionFailedError("successHandler should not have been called")
        }

        val expected = mock<ReactNativeFetchResponse>()
        val expectedBaseNetworkError = mock<WPAPINetworkError>()
        val errorHandler = { error: BaseNetworkError ->
            if (error != expectedBaseNetworkError) fail("expected error was not passed to errorHandler")
            expected
        }

        val mockedRestCallResponse = Error<JsonElement>(expectedBaseNetworkError)
        verifyPOSTRequest(successHandler, errorHandler, mockedRestCallResponse, expected)
    }

    private suspend fun verifyGETRequest(
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

        val actual = subject.getRequest(url, params, successHandler, errorHandler)
        assertEquals(expected, actual)
    }

    private suspend fun verifyPOSTRequest(
        successHandler: (JsonElement?) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        expectedRestCallResponse: WPAPIResponse<JsonElement>,
        expected: ReactNativeFetchResponse
    ) {
        whenever(wpApiGsonRequestBuilder.syncPostRequest(
            subject,
            url,
            body,
            JsonElement::class.java)
        ).thenReturn(expectedRestCallResponse)

        val actual = subject.postRequest(url, body, successHandler, errorHandler)
        assertEquals(expected, actual)
    }
}
