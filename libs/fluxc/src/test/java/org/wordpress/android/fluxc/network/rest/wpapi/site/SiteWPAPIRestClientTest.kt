package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionState
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusFetcher
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.test

// Robolectric: the fetch runs the site URL through UrlUtils.addUrlSchemeIfNeeded, which calls into
// android.webkit.URLUtil, and this module doesn't enable returnDefaultValues.
@RunWith(RobolectricTestRunner::class)
class SiteWPAPIRestClientTest {
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder = mock()
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient = mock()
    private val jetpackConnectionStatusFetcher: JetpackConnectionStatusFetcher = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var restClient: SiteWPAPIRestClient

    @Before
    fun setUp() {
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(any())).thenReturn(API_ROOT_URL)
        restClient = SiteWPAPIRestClient(
            wpapiGsonRequestBuilder,
            discoveryWPAPIRestClient,
            jetpackConnectionStatusFetcher,
            dispatcher,
            requestQueue,
            userAgent
        )
    }

    @Test
    fun `given the jetpack namespace, when the site is connected, then the wpcom id is stored`() = test {
        stubRootResponse(namespaces = listOf("wp/v2", "jetpack/v4"))
        stubConnectionState(JetpackConnectionState(isConnected = true, wpComSiteId = WPCOM_SITE_ID))

        val result = restClient.fetchWPAPISite(applicationPasswordPayload())

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_SITE_ID)
    }

    @Test
    fun `given the jetpack namespace, when the site is not connected, then there is no wpcom id`() = test {
        stubRootResponse(namespaces = listOf("wp/v2", "jetpack/v4"))
        stubConnectionState(JetpackConnectionState(isConnected = false, wpComSiteId = null))

        val result = restClient.fetchWPAPISite(applicationPasswordPayload())

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.isJetpackConnected).isFalse
        assertThat(result.siteId).isEqualTo(0L)
    }

    @Test
    fun `given no jetpack namespace, then the stored jetpack state is cleared without a status request`() = test {
        stubRootResponse(namespaces = listOf("wp/v2"))

        val result = restClient.fetchWPAPISite(
            payload = applicationPasswordPayload(),
            previousSite = connectedSite()
        )

        assertThat(result.isJetpackInstalled).isFalse
        assertThat(result.isJetpackConnected).isFalse
        assertThat(result.siteId).isEqualTo(0L)
        verifyNoInteractions(jetpackConnectionStatusFetcher)
    }

    @Test
    fun `given an unreadable connection status, then the previously known jetpack state is kept`() = test {
        stubRootResponse(namespaces = listOf("wp/v2", "jetpack/v4"))
        stubConnectionState(null)

        val result = restClient.fetchWPAPISite(
            payload = applicationPasswordPayload(),
            previousSite = connectedSite()
        )

        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_SITE_ID)
    }

    @Test
    fun `given no application password, then the connection status is not requested`() = test {
        stubRootResponse(namespaces = listOf("wp/v2", "jetpack/v4"))

        val result = restClient.fetchWPAPISite(
            FetchWPAPISitePayload(
                url = SITE_URL,
                username = USERNAME,
                password = PASSWORD,
                isApplicationPassword = false
            )
        )

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.isJetpackConnected).isFalse
        verifyNoInteractions(jetpackConnectionStatusFetcher)
    }

    @Test
    fun `when refreshing a stored site, then its local id is carried onto the fetched model`() = test {
        stubRootResponse(namespaces = listOf("wp/v2"))

        val result = restClient.fetchWPAPISite(connectedSite())

        assertThat(result.id).isEqualTo(LOCAL_ID)
    }

    private suspend fun stubRootResponse(namespaces: List<String>) {
        whenever(
            wpapiGsonRequestBuilder.syncGetRequest(
                restClient = any(),
                url = any(),
                params = any(),
                body = any(),
                clazz = eq(RootWPAPIRestResponse::class.java),
                enableCaching = any(),
                cacheTimeToLive = any(),
                nonce = anyOrNull(),
                headers = any()
            )
        ).thenReturn(Success(RootWPAPIRestResponse(name = "Site", namespaces = namespaces)))
    }

    private suspend fun stubConnectionState(state: JetpackConnectionState?) {
        whenever(jetpackConnectionStatusFetcher.fetch(any(), any(), any())).thenReturn(state)
    }

    private fun applicationPasswordPayload() = FetchWPAPISitePayload(
        url = SITE_URL,
        username = USERNAME,
        password = PASSWORD,
        isApplicationPassword = true
    )

    private fun connectedSite() = SiteModel().apply {
        id = LOCAL_ID
        url = SITE_URL
        siteId = WPCOM_SITE_ID
        apiRestUsernamePlain = USERNAME
        apiRestPasswordPlain = PASSWORD
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
    }

    companion object {
        private const val SITE_URL = "https://site.com"
        private const val API_ROOT_URL = "https://site.com/wp-json"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val LOCAL_ID = 7
        private const val WPCOM_SITE_ID = 123456L
    }
}
