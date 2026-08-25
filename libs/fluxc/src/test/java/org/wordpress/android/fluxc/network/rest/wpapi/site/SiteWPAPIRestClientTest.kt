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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionState
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusFetcher
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.test
import java.lang.reflect.Type

/**
 * Robolectric is required because libs/fluxc doesn't set returnDefaultValues and the fetch runs the site
 * URL through UrlUtils.addUrlSchemeIfNeeded, which calls android.webkit.URLUtil.
 */
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
        restClient = SiteWPAPIRestClient(
            wpapiGsonRequestBuilder,
            discoveryWPAPIRestClient,
            jetpackConnectionStatusFetcher,
            dispatcher,
            requestQueue,
            userAgent
        )
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(any())).thenReturn(API_ROOT_URL)
    }

    @Test
    fun `given the Jetpack plugin is active, then it is installed and its version is recorded`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin()))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.jetpackVersion).isEqualTo(JETPACK_VERSION)
    }

    /**
     * The jetpack/ namespace comes from the shared connection package bundled in Boost, Protect, Social and
     * VaultPress Backup, so it is not on its own evidence that the Jetpack plugin is installed.
     */
    @Test
    fun `given only a Jetpack-family plugin, then the Jetpack plugin is not installed`() = test {
        initResponse(
            namespaces = JETPACK_NAMESPACES,
            plugins = listOf(jetpackPlugin(plugin = "jetpack-boost/jetpack-boost"))
        )

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isFalse
    }

    @Test
    fun `given the Jetpack plugin is inactive, then it is not reported as installed`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin(status = "inactive")))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isFalse
    }

    @Test
    fun `given no Jetpack namespace, then the plugin list is not requested`() = test {
        initResponse(namespaces = listOf("wp/v2"))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackInstalled).isFalse
        verify(wpapiGsonRequestBuilder, never()).syncGetRequest<List<PluginResponseModel>>(
            eq(restClient), any(), any(), any(), any<Type>(), any(), any(), anyOrNull(), any()
        )
    }

    @Test
    fun `given the plugin list can't be read, then the stored Jetpack state is left alone`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, pluginsFail = true)
        val storedSite = storedSite().apply {
            setIsJetpackInstalled(true)
            jetpackVersion = JETPACK_VERSION
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackInstalled).isTrue
        assertThat(result.jetpackVersion).isEqualTo(JETPACK_VERSION)
    }

    @Test
    fun `the WordPress-com connection and blog id are always carried forward`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin()))
        val storedSite = storedSite().apply {
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_BLOG_ID)
    }

    /**
     * Deactivating the plugin must not cost the site its connection: this fetch has no view of
     * WordPress.com, and the wipe would be unrecoverable because the next fetch carries forward from it.
     */
    @Test
    fun `given Jetpack is gone, then the connection and blog id survive`() = test {
        initResponse(namespaces = listOf("wp/v2"))
        val storedSite = storedSite().apply {
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackInstalled).isFalse
        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_BLOG_ID)
    }

    @Test
    fun `given the site reports a Jetpack connection, then the blog id is stored`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin()))
        whenever(jetpackConnectionStatusFetcher.fetch(any(), any(), any()))
            .thenReturn(JetpackConnectionState(isConnected = true, wpComSiteId = WPCOM_BLOG_ID))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_BLOG_ID)
    }

    @Test
    fun `given the connection state can't be read, then the stored connection is left alone`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin()))
        whenever(jetpackConnectionStatusFetcher.fetch(any(), any(), any())).thenReturn(null)
        val storedSite = storedSite().apply {
            setIsJetpackConnected(true)
            siteId = WPCOM_BLOG_ID
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.isJetpackConnected).isTrue
        assertThat(result.siteId).isEqualTo(WPCOM_BLOG_ID)
    }

    @Test
    fun `when refreshing a stored site, then the local id is carried onto the fetched model`() = test {
        initResponse(namespaces = listOf("wp/v2"))

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.id).isEqualTo(LOCAL_SITE_ID)
    }

    @Test
    fun `given no stored site, then the connection defaults to disconnected`() = test {
        initResponse(namespaces = JETPACK_NAMESPACES, plugins = listOf(jetpackPlugin()))

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

    private suspend fun initResponse(
        namespaces: List<String>,
        plugins: List<PluginResponseModel>? = null,
        pluginsFail: Boolean = false
    ) {
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

        whenever(
            wpapiGsonRequestBuilder.syncGetRequest<List<PluginResponseModel>>(
                eq(restClient),
                any(),
                any(),
                any(),
                any<Type>(),
                any(),
                any(),
                anyOrNull(),
                any()
            )
        ).thenReturn(if (pluginsFail) Error(mock<WPAPINetworkError>()) else Success(plugins.orEmpty()))
    }

    private fun jetpackPlugin(
        plugin: String = "jetpack/jetpack",
        status: String = "active",
        version: String? = JETPACK_VERSION
    ) = PluginResponseModel(
        plugin = plugin,
        status = status,
        name = "Jetpack",
        pluginUri = null,
        author = null,
        authorUri = null,
        description = null,
        version = version,
        networkOnly = false,
        requiresWp = null,
        requiresPhp = null,
        textDomain = "jetpack"
    )

    companion object {
        private const val LOCAL_SITE_ID = 7
        private const val SITE_URL = "https://example.com"
        private const val API_ROOT_URL = "https://example.com/wp-json/"
        private const val SITE_NAME = "Example"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val JETPACK_VERSION = "14.5"
        private const val WPCOM_BLOG_ID = 256869186L
        private val JETPACK_NAMESPACES = listOf("wp/v2", "jetpack/v4")
    }
}
