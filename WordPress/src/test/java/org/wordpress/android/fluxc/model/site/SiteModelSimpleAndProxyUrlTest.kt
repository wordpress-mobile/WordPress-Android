package org.wordpress.android.fluxc.model.site

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteModelSimpleAndProxyUrlTest {
    /* isWPComSimpleSite */
    @Test
    fun `wpcom non-atomic site is a simple site`() {
        val site = wpComSite().apply { setIsWPComAtomic(false) }

        assertTrue(site.isWPComSimpleSite)
    }

    @Test
    fun `wpcom atomic site is not a simple site`() {
        val site = wpComSite().apply { setIsWPComAtomic(true) }

        assertFalse(site.isWPComSimpleSite)
    }

    @Test
    fun `self-hosted site is not a simple site`() {
        assertFalse(selfHostedSite().isWPComSimpleSite)
    }

    @Test
    fun `jetpack site is not a simple site`() {
        assertFalse(jetpackRestSite().isWPComSimpleSite)
    }

    /* getWpApiRestUrl — proxy URL for simple sites */
    @Test
    fun `atomic site returns stored wpApiRestUrl`() {
        val site = wpComSite().apply {
            setIsWPComAtomic(true)
            wpApiRestUrl = "https://atomic.example.com/wp-json/"
        }

        assertEquals("https://atomic.example.com/wp-json/", site.wpApiRestUrl)
    }

    @Test
    fun `self-hosted site with no wpApiRestUrl returns null`() {
        assertNull(selfHostedSite().wpApiRestUrl)
    }

    @Test
    fun `atomic site with no stored wpApiRestUrl returns null`() {
        val site = wpComSite().apply { setIsWPComAtomic(true) }

        assertNull(site.wpApiRestUrl)
    }

    @Test
    fun `simple site proxy URL uses wpcom siteId not local id`() {
        val site = wpComSite().apply {
            setIsWPComAtomic(false)
            siteId = 99887766
            id = 42
        }

        assertEquals(
            "https://public-api.wordpress.com/wp/v2/sites/99887766",
            site.wpApiRestUrl
        )
    }

    @Test
    fun `simple site proxy URL overrides stored wpApiRestUrl`() {
        val site = wpComSite().apply {
            setIsWPComAtomic(false)
            wpApiRestUrl = "https://should-be-ignored.example.com/wp-json/"
        }

        assertEquals(
            "https://public-api.wordpress.com/wp/v2/sites/${site.siteId}",
            site.wpApiRestUrl
        )
    }

    @Test
    fun `jetpack REST site returns stored url not proxy`() {
        val site = jetpackRestSite().apply {
            wpApiRestUrl = "https://jetpack.example.com/wp-json/"
        }

        assertTrue(site.isUsingWpComRestApi)
        assertFalse(site.isWPComSimpleSite)
        assertEquals(
            "https://jetpack.example.com/wp-json/",
            site.wpApiRestUrl
        )
    }

    private fun wpComSite() = SiteModel().apply {
        siteId = 556
        setIsWPCom(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private fun selfHostedSite() = SiteModel().apply {
        selfHostedSiteId = 6
        setIsWPCom(false)
        setIsJetpackInstalled(false)
        setIsJetpackConnected(false)
        url = "http://some.url"
        setXmlRpcUrl("http://some.url/xmlrpc.php")
        origin = SiteModel.ORIGIN_XMLRPC
    }

    private fun jetpackRestSite() = SiteModel().apply {
        siteId = 5623
        setIsWPCom(false)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        url = "http://jetpack2.url"
        setXmlRpcUrl("http://jetpack2.url/xmlrpc.php")
        origin = SiteModel.ORIGIN_WPCOM_REST
    }
}
