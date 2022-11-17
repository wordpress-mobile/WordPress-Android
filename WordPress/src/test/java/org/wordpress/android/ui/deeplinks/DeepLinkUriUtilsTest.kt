package org.wordpress.android.ui.deeplinks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.UriUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class DeepLinkUriUtilsTest {
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var uriUtilsWrapper: UriUtilsWrapper
    private lateinit var deepLinkUriUtils: DeepLinkUriUtils
    private val host = "example.com"
    lateinit var site: SiteModel

    @Before
    fun setUp() {
        deepLinkUriUtils = DeepLinkUriUtils(siteStore, uriUtilsWrapper)
        site = SiteModel()
        val buildUri = buildUri("example.com")
        whenever(uriUtilsWrapper.parse(host)).thenReturn(buildUri)
    }

    @Test
    fun `converts host to site when site exists and host matches URL`() {
        site.url = host
        whenever(siteStore.getSitesByNameOrUrlMatching(host)).thenReturn(listOf(site))

        val hostToSite = deepLinkUriUtils.hostToSite(host)

        assertThat(hostToSite).isEqualTo(site)
    }

    @Test
    fun `returns null when site exists but host doesn't match URL`() {
        site.url = host
        val differentUrl = "different_url.com"
        whenever(siteStore.getSitesByNameOrUrlMatching(differentUrl)).thenReturn(listOf(site))

        val hostToSite = deepLinkUriUtils.hostToSite(differentUrl)

        assertThat(hostToSite).isNull()
    }

    @Test
    fun `returns null when site doesnt exist`() {
        whenever(siteStore.getSitesByNameOrUrlMatching(host)).thenReturn(listOf())

        val hostToSite = deepLinkUriUtils.hostToSite(host)

        assertThat(hostToSite).isNull()
    }
}
