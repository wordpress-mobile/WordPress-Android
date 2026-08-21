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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.test

/**
 * Robolectric is required because libs/fluxc doesn't set returnDefaultValues and the fetch runs the site
 * URL through UrlUtils.addUrlSchemeIfNeeded, which calls android.webkit.URLUtil.
 */
@RunWith(RobolectricTestRunner::class)
class SiteWPAPIRestClientTest {
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder = mock()
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var restClient: SiteWPAPIRestClient

    @Before
    fun setUp() {
        restClient = SiteWPAPIRestClient(
            wpapiGsonRequestBuilder,
            discoveryWPAPIRestClient,
            dispatcher,
            requestQueue,
            userAgent
        )
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(any())).thenReturn(API_ROOT_URL)
    }

    @Test
    fun `given the Jetpack namespace is present, then Jetpack is installed`() = test {
        initResponse(namespaces = listOf("wp/v2", "jetpack/v4"))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isTrue
    }

    @Test
    fun `given the Jetpack namespace is absent, then Jetpack is not installed`() = test {
        initResponse(namespaces = listOf("wp/v2", "wc/v3"))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isFalse
    }

    @Test
    fun `given Jetpack is still present, then the stored connection and blog id are carried forward`() = test {
        initResponse(namespaces = listOf("wp/v2", "jetpack/v4"))
        val storedSite = storedSite().apply {
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_BLOG_ID)
    }

    @Test
    fun `given Jetpack is gone, then the stored connection and blog id are cleared`() = test {
        initResponse(namespaces = listOf("wp/v2"))
        val storedSite = storedSite().apply {
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackConnected).isFalse
        assertThat(result.siteId).isEqualTo(0L)
    }

    @Test
    fun `when refreshing a stored site, then the local id is carried onto the fetched model`() = test {
        initResponse(namespaces = listOf("wp/v2"))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.id).isEqualTo(LOCAL_SITE_ID)
    }

    @Test
    fun `given no stored site, then the connection defaults to disconnected`() = test {
        initResponse(namespaces = listOf("wp/v2", "jetpack/v4"))

        val result = restClient.fetchWPAPISite(
            FetchWPAPISitePayload(
                url = SITE_URL,
                username = USERNAME,
                password = PASSWORD,
                isApplicationPassword = true
            )
        )

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.isJetpackConnected).isFalse
        assertThat(result.siteId).isEqualTo(0L)
    }

    private fun storedSite() = SiteModel().apply {
        id = LOCAL_SITE_ID
        url = SITE_URL
        apiRestUsernamePlain = USERNAME
        apiRestPasswordPlain = PASSWORD
    }

    private suspend fun initResponse(namespaces: List<String>) {
        whenever(
            wpapiGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                any(),
                any(),
                any(),
                eq(RootWPAPIRestResponse::class.java),
                any(),
                any(),
                anyOrNull(),
                any()
            )
        ).thenReturn(Success(RootWPAPIRestResponse(name = SITE_NAME, namespaces = namespaces)))
    }

    companion object {
        private const val LOCAL_SITE_ID = 7
        private const val SITE_URL = "https://example.com"
        private const val API_ROOT_URL = "https://example.com/wp-json/"
        private const val SITE_NAME = "Example"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val WPCOM_BLOG_ID = 256869186L
    }
}
