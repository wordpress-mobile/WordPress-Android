package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class WPApiApplicationPasswordsRestClientTest {
    private val applicationName = "name"
    private val testSite = SiteModel().apply {
        url = "http://test-site.com"
        username = "username"
        password = "password"
    }

    private val cookieNonceAuthenticator: CookieNonceAuthenticator = mock()

    private lateinit var restClient: WPApiApplicationPasswordsRestClient

    @Before
    fun setup() {
        restClient = WPApiApplicationPasswordsRestClient(
            wpApiGsonRequestBuilder = mock(),
            cookieNonceAuthenticator = cookieNonceAuthenticator,
            noCookieRequestQueue = mock(),
            requestQueue = mock(),
            dispatcher = mock(),
            userAgent = mock()
        )
    }

    @Test
    fun `given a valid response, when creating a password, then return the credentials`() = runTest {
        givenCreationResponse(
            WPAPIResponse.Success(
                ApplicationPasswordCreationResponse(uuid = "uuid", name = applicationName, password = "pwd")
            )
        )

        val payload = restClient.createApplicationPassword(testSite, applicationName)

        assertFalse(payload.isError)
        assertEquals("pwd", payload.password)
        assertEquals("uuid", payload.uuid)
    }

    @Test
    fun `given a response with a null password, when creating a password, then return an error`() = runTest {
        givenCreationResponse(
            WPAPIResponse.Success(
                ApplicationPasswordCreationResponse(uuid = "uuid", name = applicationName, password = null)
            )
        )

        val payload = restClient.createApplicationPassword(testSite, applicationName)

        assertTrue(payload.isError)
        assertEquals("Password or UUID missing from response", payload.error.message)
    }

    @Test
    fun `given a response with a null uuid, when creating a password, then return an error`() = runTest {
        givenCreationResponse(
            WPAPIResponse.Success(
                ApplicationPasswordCreationResponse(uuid = null, name = applicationName, password = "pwd")
            )
        )

        val payload = restClient.createApplicationPassword(testSite, applicationName)

        assertTrue(payload.isError)
        assertEquals("Password or UUID missing from response", payload.error.message)
    }

    @Test
    fun `given a null response body, when creating a password, then return an error`() = runTest {
        givenCreationResponse(WPAPIResponse.Success(null))

        val payload = restClient.createApplicationPassword(testSite, applicationName)

        assertTrue(payload.isError)
        assertEquals("Password or UUID missing from response", payload.error.message)
    }

    @Test
    fun `given a network error, when creating a password, then propagate the error`() = runTest {
        val error = WPAPINetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR))
        givenCreationResponse(WPAPIResponse.Error(error))

        val payload = restClient.createApplicationPassword(testSite, applicationName)

        assertTrue(payload.isError)
        assertEquals(error, payload.error)
    }

    private suspend fun givenCreationResponse(response: WPAPIResponse<ApplicationPasswordCreationResponse>) {
        whenever(
            cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest<WPAPIResponse<ApplicationPasswordCreationResponse>>(
                eq(testSite),
                any()
            )
        ).thenReturn(response)
    }
}
