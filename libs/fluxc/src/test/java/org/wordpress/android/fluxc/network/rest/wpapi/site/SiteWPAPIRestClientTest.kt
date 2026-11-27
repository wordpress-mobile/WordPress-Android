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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusFetcher
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.test
import org.robolectric.RobolectricTestRunner

/**
 * Covers the carry-forward contract of [SiteWPAPIRestClient.fetchWPAPISite]: the model it returns
 * replaces the stored row wholesale, so anything this fetch can't determine has to survive it.
 */
@RunWith(RobolectricTestRunner::class)
class SiteWPAPIRestClientTest {
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder = mock()
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient = mock()
    private val jetpackConnectionStatusFetcher: JetpackConnectionStatusFetcher = mock()
    private val siteOptionsRsFetcher: SiteOptionsRsFetcher = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()
    private lateinit var restClient: SiteWPAPIRestClient

    @Before
    fun setUp() {
        whenever(discoveryWPAPIRestClient.discoverWPAPIBaseURL(any())).thenReturn(WP_API_URL)
        restClient = SiteWPAPIRestClient(
            wpapiGsonRequestBuilder,
            discoveryWPAPIRestClient,
            jetpackConnectionStatusFetcher,
            siteOptionsRsFetcher,
            dispatcher,
            requestQueue,
            userAgent
        )
    }

    @Test
    fun `refresh carries forward the fields the root response cannot determine`() = test {
        givenRootResponse(RootWPAPIRestResponse(name = "Fresh title"))
        givenSiteOptions(SiteOptions())
        val storedSite = storedSite()

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.name).isEqualTo("Fresh title")
        // Only wp.getUsersBlogs ever sets these two, so losing them would be unrecoverable.
        assertThat(result.selfHostedSiteId).isEqualTo(42L)
        assertThat(result.isSelfHostedAdmin).isTrue()
        assertThat(result.softwareVersion).isEqualTo("6.7")
        assertThat(result.xmlRpcUrl).isEqualTo("$SITE_URL/xmlrpc.php")
        assertThat(result.loginUrl).isEqualTo("$SITE_URL/wp-login.php")
        assertThat(result.isFeaturedImageSupported).isTrue()
        assertThat(result.defaultCommentStatus).isEqualTo("closed")
    }

    @Test
    fun `refresh keeps the stored origin so comments and post formats keep routing as before`() = test {
        givenRootResponse(RootWPAPIRestResponse())
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.origin).isEqualTo(SiteModel.ORIGIN_XMLRPC)
    }

    @Test
    fun `first fetch of a site with no stored row is stamped as WPAPI`() = test {
        givenRootResponse(RootWPAPIRestResponse(name = "New site"))

        val result = restClient.fetchWPAPISite(applicationPasswordPayload())

        assertThat(result.origin).isEqualTo(SiteModel.ORIGIN_WPAPI)
        assertThat(result.name).isEqualTo("New site")
        // There's no row to read the options off yet.
        verify(siteOptionsRsFetcher, never()).fetch(any())
    }

    @Test
    fun `a root response without a name leaves the stored name alone`() = test {
        givenRootResponse(RootWPAPIRestResponse(name = null))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.name).isEqualTo("Stored title")
    }

    @Test
    fun `an empty name in the root response leaves the stored name alone`() = test {
        givenRootResponse(RootWPAPIRestResponse(name = ""))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.name).isEqualTo("Stored title")
    }

    @Test
    fun `site options the site refused to report leave the stored values alone`() = test {
        givenRootResponse(RootWPAPIRestResponse(gmtOffset = null))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.defaultCommentStatus).isEqualTo("closed")
        assertThat(result.isFeaturedImageSupported).isTrue()
        assertThat(result.timezone).isEqualTo("-8")
    }

    @Test
    fun `site options the site reported are applied`() = test {
        givenRootResponse(RootWPAPIRestResponse(gmtOffset = "2"))
        givenSiteOptions(
            SiteOptions(
                defaultCommentStatus = "open",
                timezone = "Europe/Madrid",
                isFeaturedImageSupported = false
            )
        )

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.defaultCommentStatus).isEqualTo("open")
        // The Olson name wins over the root response's numeric offset: it's the DST-correct one.
        assertThat(result.timezone).isEqualTo("Europe/Madrid")
        assertThat(result.isFeaturedImageSupported).isFalse()
    }

    @Test
    fun `the root response's gmt offset is used when the settings endpoint reports no timezone`() = test {
        givenRootResponse(RootWPAPIRestResponse(gmtOffset = "2"))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.timezone).isEqualTo("2")
    }

    @Test
    fun `an unreadable namespace list leaves the stored WooCommerce flag alone`() = test {
        givenRootResponse(RootWPAPIRestResponse(namespaces = null))
        givenSiteOptions(SiteOptions())
        val storedSite = storedSite().apply { hasWooCommerce = true }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.hasWooCommerce).isTrue()
    }

    @Test
    fun `the index timezone string is preferred over the numeric gmt offset`() = test {
        // The index serves timezone_string unauthenticated, so even a non-administrator gets the
        // DST-correct zone and never falls back to the fixed offset. See CMM-1095.
        givenRootResponse(RootWPAPIRestResponse(timezoneString = "Europe/Madrid", gmtOffset = "2"))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.timezone).isEqualTo("Europe/Madrid")
    }

    @Test
    fun `an empty index timezone string falls back to the gmt offset`() = test {
        // A site on a manual UTC offset reports timezone_string as empty.
        givenRootResponse(RootWPAPIRestResponse(timezoneString = "", gmtOffset = "2"))
        givenSiteOptions(SiteOptions())

        val result = restClient.fetchWPAPISite(storedSite())

        assertThat(result.timezone).isEqualTo("2")
    }

    @Test
    fun `a legacy row with a working XMLRPC endpoint keeps its unknown origin`() = test {
        // ORIGIN_WPAPI would tell CommentsStore and fetchPostFormats that XML-RPC is unavailable.
        givenRootResponse(RootWPAPIRestResponse())
        givenSiteOptions(SiteOptions())
        val storedSite = storedSite().apply { origin = SiteModel.ORIGIN_UNKNOWN }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.origin).isEqualTo(SiteModel.ORIGIN_UNKNOWN)
    }

    @Test
    fun `a legacy row with no XMLRPC endpoint is healed to WPAPI`() = test {
        givenRootResponse(RootWPAPIRestResponse())
        givenSiteOptions(SiteOptions())
        val storedSite = storedSite().apply {
            origin = SiteModel.ORIGIN_UNKNOWN
            xmlRpcUrl = ""
        }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.origin).isEqualTo(SiteModel.ORIGIN_WPAPI)
    }

    @Test
    fun `the stored rest endpoint is used instead of rediscovering one`() = test {
        givenRootResponse(RootWPAPIRestResponse(name = "Fresh title"))
        givenSiteOptions(SiteOptions())
        val storedSite = storedSite().apply { wpApiRestUrl = WP_API_URL }

        val result = restClient.fetchWPAPISite(storedSite)

        assertThat(result.wpApiRestUrl).isEqualTo(WP_API_URL)
        verify(discoveryWPAPIRestClient, never()).discoverWPAPIBaseURL(any())
    }

    private suspend fun givenRootResponse(response: RootWPAPIRestResponse) {
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
        ).thenReturn(WPAPIResponse.Success(response))
    }

    private suspend fun givenSiteOptions(siteOptions: SiteOptions) {
        whenever(siteOptionsRsFetcher.fetch(any())).thenReturn(siteOptions)
    }

    /** A row as the application-password sign-in leaves it: XML-RPC origin, REST credentials. */
    private fun storedSite() = SiteModel().apply {
        id = 7
        setIsWPCom(false)
        origin = SiteModel.ORIGIN_XMLRPC
        url = SITE_URL
        name = "Stored title"
        selfHostedSiteId = 42L
        setIsSelfHostedAdmin(true)
        softwareVersion = "6.7"
        xmlRpcUrl = "$SITE_URL/xmlrpc.php"
        loginUrl = "$SITE_URL/wp-login.php"
        setIsFeaturedImageSupported(true)
        defaultCommentStatus = "closed"
        timezone = "-8"
        apiRestUsernamePlain = "user"
        apiRestPasswordPlain = "app password"
    }

    private fun applicationPasswordPayload() = FetchWPAPISitePayload(
        url = SITE_URL,
        username = "user",
        password = "app password",
        isApplicationPassword = true
    )

    companion object {
        private const val SITE_URL = "https://site.com"
        private const val WP_API_URL = "$SITE_URL/wp-json/"
    }
}
