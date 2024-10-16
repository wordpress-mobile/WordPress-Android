package org.wordpress.android.fluxc.network.rest.wpcom.mobile

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import junit.framework.AssertionFailedError
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
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
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload
import org.wordpress.android.fluxc.test

class JetpackMigrationRestClientTest {
    private val wpComGsonRequestBuilder = mock<WPComGsonRequestBuilder>()
    private val context = mock<Context>()
    private val dispatcher = mock<Dispatcher>()
    private val requestQueue = mock<RequestQueue>()
    private val accessToken = mock<AccessToken>()
    private val userAgent = mock<UserAgent>()

    private lateinit var client: JetpackMigrationRestClient
    private lateinit var urlCaptor: KArgumentCaptor<String>

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        client = JetpackMigrationRestClient(
                wpComGsonRequestBuilder,
                dispatcher,
                context,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `fetch handles successful response`() = test {
        val errorHandler: (BaseNetworkError?) -> MigrationCompleteFetchedPayload = { _ ->
            throw AssertionFailedError("errorHandler should not have been called")
        }

        val expected = MigrationCompleteFetchedPayload.Success
        val expectedJson = mock<JsonElement>()

        val expectedRestCallResponse = Success(expectedJson)
        verifyRestApi(errorHandler, expectedRestCallResponse, expected)
    }

    @Test
    fun `fetch handles failure response`() = test {
        val expected = mock<MigrationCompleteFetchedPayload>()
        val expectedBaseNetworkError = mock<WPComGsonNetworkError>()
        val errorHandler = { error: BaseNetworkError? ->
            if (error != expectedBaseNetworkError) fail("expected error was not passed to errorHandler")
            expected
        }

        val mockedRestCallResponse = Error<JsonElement>(expectedBaseNetworkError)
        verifyRestApi(errorHandler, mockedRestCallResponse, expected)
    }

    private suspend fun verifyRestApi(
        errorHandler: (BaseNetworkError?) -> MigrationCompleteFetchedPayload,
        expectedRestCallResponse: WPComGsonRequestBuilder.Response<JsonElement>,
        expected: MigrationCompleteFetchedPayload
    ) {
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(client),
                        urlCaptor.capture(),
                        eq(mapOf()),
                        eq(mapOf()),
                        eq(JsonElement::class.java),
                        isNull(),
                        anyOrNull(),
                )
        ).thenReturn(expectedRestCallResponse)

        val actual = client.migrationComplete(errorHandler)
        assertEquals(expected, actual)
    }
}
