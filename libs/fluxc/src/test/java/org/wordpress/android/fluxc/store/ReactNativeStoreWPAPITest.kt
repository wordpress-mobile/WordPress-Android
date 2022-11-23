package org.wordpress.android.fluxc.store

import android.net.Uri
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.NonceRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ReactNativeStoreWPAPITest {
    private val queryKey = "a_key"
    private val queryValue = "a_value"
    private val restPath = "rest_path"
    private val restPathWithParams = "$restPath?$queryKey=$queryValue"
    private val paramMap = mapOf(queryKey to queryValue)
    private val currentTime = 1000000000L

    private val wpApiRestClient = mock<ReactNativeWPAPIRestClient>()
    private val discoveryWPAPIRestClient = mock<DiscoveryWPAPIRestClient>()
    private val nonceRestClient = mock<NonceRestClient>()

    private lateinit var store: ReactNativeStore
    private lateinit var site: SiteModel

    private interface SitePersister : (SiteModel) -> Int

    private lateinit var sitePersistenceMock: SitePersister

    @Before
    fun setup() {
        site = SiteModel().apply {
            url = "https://site_url.com/mysite"
            wpApiRestUrl = "http://site_url.com/mysite/a_url_path_with_a_custom_rest_api_extension"
        }
        initStore(null)
    }

    private fun initStore(nonce: Nonce?) {
        whenever(nonceRestClient.getNonce(any())).thenReturn(nonce)
        sitePersistenceMock = mock()
        store = ReactNativeStore(
                mock(),
                wpApiRestClient,
                nonceRestClient,
                discoveryWPAPIRestClient,
                TestSiteSqlUtils.siteSqlUtils,
                initCoroutineEngine(),
                { currentTime },
                sitePersistenceMock
        )
    }

    @Test
    fun `discovers rest endpoint, authenticates, and performs fetch`() = test {
        // no saved endpoint
        site.wpApiRestUrl = null

        // discovers proper rest url since no saved endpoint
        val restUrl = "a_url_path_with_a_custom_rest_api_extension"
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url))
                .thenReturn(restUrl)

        // retrieves nonce
        val nonce = Available("a_nonce")
        whenever(nonceRestClient.requestNonce(site))
                .thenReturn(nonce)

        // uses updated nonce to make successful call
        val callWithSuccess = mock<Success>()
        val fetchUrl = "$restUrl/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl, nonce.value))
                .thenReturn(callWithSuccess)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(callWithSuccess, actualResponse)
        assertEquals(restUrl, site.wpApiRestUrl, "site should be updated with rest endpoint used for successful call")
        inOrder(discoveryWPAPIRestClient, sitePersistenceMock, wpApiRestClient, nonceRestClient) {
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL(site.url)
            verify(sitePersistenceMock)(site) // persist site after discovering wpApiRestUrl
            verify(nonceRestClient).requestNonce(site)
            verify(wpApiRestClient).fetch(fetchUrl, nonce.value)
        }
    }

    @Test
    fun `uses saved rest endpoint if available`() = test {
        // site has wpApiRestEndpoint, so uses that instead of discovering endpoint with api call
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"

        val initialResponseWithSuccess = mock<Success>()
        whenever(wpApiRestClient.fetch(fetchUrl))
                .thenReturn(initialResponseWithSuccess)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(initialResponseWithSuccess, actualResponse)
        verify(wpApiRestClient).fetch(fetchUrl)
        verify(sitePersistenceMock, never())(any()) // no wpApiRestUrl updates, so no persistence
        verify(discoveryWPAPIRestClient, never()).discoverWPAPIBaseURL(any())
    }

    @Test
    fun `uses discovery if no saved rest endpoint`() = test {
        // no saved endpoint
        site.wpApiRestUrl = null

        // discovers rest endpoint because site.wpApiRestEndpoint is null
        val restUrl = "a_url_path_with_a_custom_rest_api_extension"
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url))
                .thenReturn(restUrl)

        // performs successful call using discovered restUrl
        val initialResponseWithSuccess = mock<Success>()
        val fetchUrl = "$restUrl/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl))
                .thenReturn(initialResponseWithSuccess)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(initialResponseWithSuccess, actualResponse)
        assertEquals(restUrl, site.wpApiRestUrl, "site should be updated with rest endpoint used for successful call")
        inOrder(discoveryWPAPIRestClient, sitePersistenceMock, wpApiRestClient) {
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL(site.url)
            verify(sitePersistenceMock)(site) // persist site after discovering wpApiRestUrl
            verify(wpApiRestClient).fetch(fetchUrl)
        }
    }

    @Test
    fun `uses default endpoint if no endpoint saved and discovery fails`() = test {
        // no saved endpoint
        site.wpApiRestUrl = null

        // discovery fails
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url))
                .thenReturn(null)

        // makes successful call using fallback rest url
        val successfulResponse = mock<Success>()
        val fallbackRestUrl = "${site.url}/wp-json/"
        val fetchUrl = "$fallbackRestUrl$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl))
                .thenReturn(successfulResponse)

        val actualResponse = store.executeRequest(site, "$restPath?$queryKey=$queryValue")
        assertEquals(successfulResponse, actualResponse)
        assertEquals(
                fallbackRestUrl, site.wpApiRestUrl,
                "site should be updated with rest endpoint used for successful call"
        )
        inOrder(discoveryWPAPIRestClient, sitePersistenceMock, wpApiRestClient) {
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL(site.url)
            verify(sitePersistenceMock)(site) // persist default endpoint after failed discovery
            verify(wpApiRestClient).fetch(fetchUrl)
        }
    }

    @Test
    fun `'not found' error after using saved endpoint, use discovery and try again`() = test {
        val incorrectRestEndpoint = "not_the_right_endpoint"
        site.wpApiRestUrl = incorrectRestEndpoint

        // does not use discovery initially because there is a saved wpApiRestEndpoint
        // call fails with not found (404) error
        val incorrectUrl = "$incorrectRestEndpoint/$restPath"
        val initialResponseWithNotFoundError = errorResponse(StatusCode.NOT_FOUND_404)
        whenever(wpApiRestClient.fetch(incorrectUrl))
                .thenReturn(initialResponseWithNotFoundError)

        // try to discover endpoint because failure was with a previously saved restUrl
        val restUrl = "a_url_path_with_a_custom_rest_api_extension"
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(any()))
                .thenReturn(restUrl)

        // second call using newly discovered rest url succeeds
        val correctUrl = "$restUrl/$restPath"
        val secondResponseWithSuccess = mock<Success>()
        whenever(wpApiRestClient.fetch(correctUrl))
                .thenReturn(secondResponseWithSuccess)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(secondResponseWithSuccess, actualResponse)
        assertEquals(restUrl, site.wpApiRestUrl, "should save rest endpoint used for successful call")
        inOrder(discoveryWPAPIRestClient, sitePersistenceMock, wpApiRestClient) {
            verify(wpApiRestClient).fetch(incorrectUrl)
            verify(sitePersistenceMock)(site) // persist site after clearing wpApiRestUrl that resulted in 404 failure
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL(site.url)
            verify(sitePersistenceMock)(site) // persist site after discovering wpApiRestUrl
            verify(wpApiRestClient).fetch(correctUrl)
        }
    }

    @Test
    fun `'not found' error after using discovery, just returns error`() = test {
        // no previously saved endpoint
        site.wpApiRestUrl = null

        // discovers proper rest url
        val restUrl = "a_url_path_with_a_custom_rest_api_extension"
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url))
                .thenReturn(restUrl)

        // call using discovered rest url fails with not found (404)
        val responseWithNotFoundError = errorResponse(StatusCode.NOT_FOUND_404)
        val fetchUrl = "$restUrl/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl))
                .thenReturn(responseWithNotFoundError)

        // 'not found' error does not lead to discovery call because we already did discovery
        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(responseWithNotFoundError, actualResponse)
        assertNull(site.wpApiRestUrl, "should not update site wpApiRestEndpoint when call fails")
        inOrder(discoveryWPAPIRestClient, sitePersistenceMock, wpApiRestClient) {
            verify(discoveryWPAPIRestClient).discoverWPAPIBaseURL(site.url)
            verify(sitePersistenceMock)(site) // persist site after discovering wpApiRestUrl
            verify(wpApiRestClient).fetch(fetchUrl)
            verify(sitePersistenceMock)(site) // persist site after clearing wpApiRestUrl that resulted in 404 failure
        }
    }

    @Test
    fun `if error is NEITHER 'not found' nor unauthenticated, returns error`() = test {
        val responseWithUnknownError = errorResponse(StatusCode.UNKNOWN)
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl))
                .thenReturn(responseWithUnknownError)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(responseWithUnknownError, actualResponse)
        verify(wpApiRestClient).fetch(fetchUrl)
    }

    //
    // Nonce tests
    //

    @Test
    fun `refreshed nonce leads to unauthorized error, so returns error`() = test {
        // nonce never requested, so retrieves nonce
        initStore(null)
        val nonce = Available("a_nonce")
        whenever(nonceRestClient.getNonce(site))
                .thenReturn(nonce)

        // initial fetch uses saved nonce and fails with unauthorized
        val initialResponseWithUnauthorizedError = errorResponse(StatusCode.UNAUTHORIZED_401)
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl, nonce.value))
                .thenReturn(initialResponseWithUnauthorizedError)

        // Already refreshed nonce, so just returns unauthorized error
        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(initialResponseWithUnauthorizedError, actualResponse)
        inOrder(wpApiRestClient) {
            verify(wpApiRestClient).fetch(fetchUrl, nonce.value)
        }
    }

    @Test
    fun `reusing saved nonce leads to unauthorized error, updates nonce but nonce is same, so returns error`() = test {
        val savedNonce = Available("saved_nonce")
        initStore(savedNonce)

        // initial fetch uses saved nonce and fails with unauthorized
        val initialResponseWithUnauthorizedError = errorResponse(StatusCode.UNAUTHORIZED_401)
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl, savedNonce.value))
                .thenReturn(initialResponseWithUnauthorizedError)

        // fetching nonce returns already used nonce
        whenever(nonceRestClient.getNonce(site))
                .thenReturn(savedNonce)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(initialResponseWithUnauthorizedError, actualResponse)
        inOrder(wpApiRestClient, nonceRestClient) {
            verify(wpApiRestClient).fetch(fetchUrl, savedNonce.value)
            verify(nonceRestClient).requestNonce(site)
        }
    }

    @Test
    fun `reusing saved nonce leads to unauthorized error, successfully updates nonce, so tries call again`() = test {
        val savedNonce = Available("saved_nonce")
        initStore(savedNonce)

        // initial fetch uses saved nonce and fails with unauthorized
        val initialResponseWithUnauthorizedError = errorResponse(StatusCode.UNAUTHORIZED_401)
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl, savedNonce.value))
                .thenReturn(initialResponseWithUnauthorizedError)

        // fetches new nonce successfully
        val updatedNonce = Available("updated_nonce")
        whenever(nonceRestClient.requestNonce(site))
                .thenReturn(updatedNonce)

        // retries original call
        val secondResponseWithSuccess = mock<Success>()
        whenever(wpApiRestClient.fetch(fetchUrl, updatedNonce.value))
                .thenReturn(secondResponseWithSuccess)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(secondResponseWithSuccess, actualResponse)
        inOrder(wpApiRestClient, nonceRestClient) {
            verify(nonceRestClient).getNonce(site)
            verify(wpApiRestClient).fetch(fetchUrl, savedNonce.value)
            verify(nonceRestClient).requestNonce(site)
            verify(wpApiRestClient).fetch(fetchUrl, updatedNonce.value)
        }
    }

    @Test
    fun `reusing saved nonce leads to unauthorized error, fails to update nonce, so returns original error`() = test {
        val savedNonce = Available("saved_nonce")
        initStore(savedNonce)

        // initial fetch uses saved nonce and fails with unauthorized
        val initialResponseWithUnauthorizedError = errorResponse(StatusCode.UNAUTHORIZED_401)
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        whenever(wpApiRestClient.fetch(fetchUrl, savedNonce.value))
                .thenReturn(initialResponseWithUnauthorizedError)

        // fails to fetch new nonce
        whenever(nonceRestClient.getNonce(site))
                .thenReturn(savedNonce, null)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(initialResponseWithUnauthorizedError, actualResponse)
        inOrder(wpApiRestClient, nonceRestClient) {
            verify(wpApiRestClient).fetch(fetchUrl, savedNonce.value)
            verify(nonceRestClient).requestNonce(site)
        }
    }

    @Test
    fun `nonce unavailable from recent request, so does not request nonce`() = test {
        // previous nonce faield, and was "recent"
        val fourMinuteOldFailedNonceRequest = FailedRequest(currentTime - 4 * 60 * 1000)
        initStore(fourMinuteOldFailedNonceRequest)

        // does not use nonce to make request because of recent unsuccessful attempt to refresh nonce
        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        val successResponse = mock<Success>()
        whenever(wpApiRestClient.fetch(fetchUrl, null)) // passes null for nonce
                .thenReturn(successResponse)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(successResponse, actualResponse)
        verify(wpApiRestClient).fetch(fetchUrl, null)
        verify(nonceRestClient, never()).requestNonce(any())
    }

    @Test
    fun `nonce unavailable from older request, so requests nonce`() = test {
        // previous nonce request failed, but was not "recent"
        val sixMinuteOldUnavailableNonce = FailedRequest(currentTime - 6 * 60 * 1000)
        initStore(sixMinuteOldUnavailableNonce)

        // refreshes nonce because latest attempt to refresh nonce was not recent
        val nonce = Available("a_nonce")
        whenever(nonceRestClient.requestNonce(site))
                .thenReturn(nonce)

        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        val successResponse = mock<Success>()
        whenever(wpApiRestClient.fetch(fetchUrl, nonce.value)) // passes null for nonce
                .thenReturn(successResponse)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(successResponse, actualResponse)
        inOrder(nonceRestClient, wpApiRestClient) {
            verify(nonceRestClient).requestNonce(site)
            verify(wpApiRestClient).fetch(fetchUrl, nonce.value)
        }
    }

    @Test
    fun `nonce unknown, so requests nonce`() = test {
        // previous nonce request unknown
        initStore(Unknown)

        // refreshes nonce because latest attempt to refresh nonce was not recent
        val nonce = Available("a_nonce")
        whenever(nonceRestClient.requestNonce(site))
                .thenReturn(nonce)

        val fetchUrl = "${site.wpApiRestUrl}/$restPath"
        val successResponse = mock<Success>()
        whenever(wpApiRestClient.fetch(fetchUrl, nonce.value)) // passes null for nonce
                .thenReturn(successResponse)

        val actualResponse = store.executeRequest(site, restPathWithParams)
        assertEquals(successResponse, actualResponse)
        inOrder(wpApiRestClient, nonceRestClient) {
            verify(nonceRestClient).requestNonce(site)
            verify(wpApiRestClient).fetch(fetchUrl, nonce.value)
        }
    }

    @Test
    fun `test slashJoin`() {
        assertEquals("begin/end", ReactNativeStore.slashJoin("begin", "end"))
        assertEquals("begin/end", ReactNativeStore.slashJoin("begin/", "end"))
        assertEquals("begin/end", ReactNativeStore.slashJoin("begin", "/end"))
        assertEquals("begin/end", ReactNativeStore.slashJoin("begin/", "/end"))
    }

    @Test
    fun `handles failure to parse path`() = test {
        val mockUri = mock<Uri>()
        assertNull(mockUri.path, "path must be null to represent failure to parse the path in this test")
        val uriParser = { _: String -> mockUri }

        store = ReactNativeStore(
                mock(),
                wpApiRestClient,
                nonceRestClient,
                discoveryWPAPIRestClient,
                TestSiteSqlUtils.siteSqlUtils,
                initCoroutineEngine(),
                { currentTime },
                sitePersistenceMock,
                uriParser
        )

        val response = store.executeRequest(mock(), "")
        val errorType = (response as? Error)?.error?.type
        assertEquals(UNKNOWN, errorType)
    }

    private suspend fun ReactNativeWPAPIRestClient.fetch(url: String, nonce: String? = null) =
            fetch(url, paramMap, ReactNativeFetchResponse::Success, ReactNativeFetchResponse::Error, nonce)

    private fun errorResponse(statusCode: Int): ReactNativeFetchResponse = Error(mock()).apply {
        error.volleyError = VolleyError(NetworkResponse(statusCode, null, false, 0L, null))
    }

    private object StatusCode {
        const val UNAUTHORIZED_401 = 401
        const val NOT_FOUND_404 = 404
        const val UNKNOWN = 99999
    }
}
