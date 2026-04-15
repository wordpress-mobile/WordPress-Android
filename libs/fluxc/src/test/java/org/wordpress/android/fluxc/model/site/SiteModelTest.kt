package org.wordpress.android.fluxc.model.site

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.site.SiteUtils
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteModelTest {
    /* Publicize support */
    @Test
    fun `given self hosted non jp site, when site is generated, publicize is disabled`() {
        val site = SiteUtils.generateSelfHostedNonJPSite()

        assertFalse(site.supportsPublicize())
    }

    @Test
    fun `given jetpack site, when site is generated over xmlrpc, publicize is disabled`() {
        val site = SiteUtils.generateJetpackSiteOverXMLRPC()

        assertFalse(site.supportsPublicize())
    }

    @Test
    fun `given site with publish posts capability disabled, when site is generated, publicize is disabled`() {
        val site = SiteUtils.generateWPComSite()
        site.hasCapabilityPublishPosts = false

        assertFalse(site.supportsPublicize())
    }

    @Test
    fun `given wpcom site with publish posts capability enabled, when site is generated, publicize is enabled`() {
        val site = SiteUtils.generateWPComSite()
        site.hasCapabilityPublishPosts = true

        assertTrue(site.supportsPublicize())
    }

    @Test
    fun `given wpcom site with publicize permanently disabled, when site is generated, publicize is disabled`() {
        val site = SiteUtils.generateWPComSite()
        site.hasCapabilityPublishPosts = true
        site.setIsPublicizePermanentlyDisabled(true)

        assertFalse(site.supportsPublicize())
    }

    @Test
    fun `given jetpack site with publicize module, when site is generated over rest, publicize is enabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.hasCapabilityPublishPosts = true
        site.activeModules = SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE

        assertTrue(site.supportsPublicize())
    }

    @Test
    fun `given jetpack site without publicize module, when site is generated over rest, publicize is disabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.hasCapabilityPublishPosts = true
        site.activeModules = ""

        assertFalse(site.supportsPublicize())
    }

    /* Share buttons support */
    @Test
    fun `given self hosted non jp site, when site is generated, share buttons is not supported`() {
        val site = SiteUtils.generateSelfHostedNonJPSite()

        assertFalse(site.supportsShareButtons())
    }

    @Test
    fun `given jetpack site, when site is generated over xmlrpc, share buttons is not supported`() {
        val site = SiteUtils.generateJetpackSiteOverXMLRPC()

        assertFalse(site.supportsShareButtons())
    }

    @Test
    fun `given any site without manage options capability, when site is generated, share buttons is not supported`() {
        val site = SiteUtils.generateWPComSite()
        site.hasCapabilityManageOptions = false

        assertFalse(site.supportsShareButtons())
    }

    @Test
    fun `given jetpack site without sharing buttons module, when site is generated, share buttons is not supported`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.hasCapabilityManageOptions = true
        site.activeModules = ""

        assertFalse(site.supportsShareButtons())
    }

    @Test
    fun `given jetpack site with sharing buttons module, when site is generated, share buttons is supported`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.hasCapabilityManageOptions = true
        site.activeModules = SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS

        assertTrue(site.supportsShareButtons())
    }

    /* Sharing support */
    @Test
    fun `given publicize supported wpcom site, when site is generated, sharing is enabled`() {
        val site = SiteUtils.generateWPComSite()
        site.setPublicizeSupport(true)

        assertTrue(site.supportsSharing())
    }

    @Test
    fun `given share buttons supported wpcom site, when site is generated, sharing is enabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.setShareButtonsSupport(true)

        assertTrue(site.supportsSharing())
    }

    @Test
    fun `given publicize + share buttons unsupported wpcom site, when site is generated, sharing is disabled`() {
        val site = SiteUtils.generateWPComSite()
        site.setPublicizeSupport(false)
        site.setShareButtonsSupport(false)

        assertFalse(site.supportsSharing())
    }

    @Test
    fun `given share buttons supported jetpack site, when site is generated over rest, sharing is enabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.setShareButtonsSupport(true)

        assertTrue(site.supportsSharing())
    }

    @Test
    fun `given publicize supported jetpack site, when site is generated over rest, sharing is enabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.setPublicizeSupport(true)

        assertTrue(site.supportsSharing())
    }

    @Test
    fun `given publicize + share btns unsupported jetpack site, when site generated over rest, sharing is disabled`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.setPublicizeSupport(false)
        site.setShareButtonsSupport(false)

        assertFalse(site.supportsSharing())
    }

    /* isWPComSimpleSite */
    @Test
    fun `wpcom non-atomic site is a simple site`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(false)

        assertTrue(site.isWPComSimpleSite)
    }

    @Test
    fun `wpcom atomic site is not a simple site`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(true)

        assertFalse(site.isWPComSimpleSite)
    }

    @Test
    fun `self-hosted site is not a simple site`() {
        val site = SiteUtils.generateSelfHostedNonJPSite()

        assertFalse(site.isWPComSimpleSite)
    }

    @Test
    fun `jetpack site is not a simple site`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()

        assertFalse(site.isWPComSimpleSite)
    }

    /* getWpApiRestUrl — proxy URL for simple sites */
    @Test
    fun `atomic site returns stored wpApiRestUrl`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(true)
        site.wpApiRestUrl = "https://atomic.example.com/wp-json/"

        assertEquals("https://atomic.example.com/wp-json/", site.wpApiRestUrl)
    }

    @Test
    fun `self-hosted site with no wpApiRestUrl returns null`() {
        val site = SiteUtils.generateSelfHostedNonJPSite()

        assertNull(site.wpApiRestUrl)
    }

    @Test
    fun `atomic site with no stored wpApiRestUrl returns null`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(true)

        assertNull(site.wpApiRestUrl)
    }

    @Test
    fun `simple site proxy URL uses wpcom siteId not local id`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(false)
        site.siteId = 99887766
        site.id = 42

        assertEquals(
            "https://public-api.wordpress.com/wp/v2/sites/99887766",
            site.wpApiRestUrl
        )
    }

    @Test
    fun `simple site proxy URL overrides stored wpApiRestUrl`() {
        val site = SiteUtils.generateWPComSite()
        site.setIsWPComAtomic(false)
        site.wpApiRestUrl = "https://should-be-ignored.example.com/wp-json/"

        assertEquals(
            "https://public-api.wordpress.com/wp/v2/sites/${site.siteId}",
            site.wpApiRestUrl
        )
    }

    @Test
    fun `jetpack REST site returns stored url not proxy`() {
        val site = SiteUtils.generateJetpackSiteOverRestOnly()
        site.wpApiRestUrl = "https://jetpack.example.com/wp-json/"

        assertTrue(site.isUsingWpComRestApi)
        assertFalse(site.isWPComSimpleSite)
        assertEquals(
            "https://jetpack.example.com/wp-json/",
            site.wpApiRestUrl
        )
    }

    private fun SiteModel.setPublicizeSupport(enablePublicizeSupport: Boolean) {
        this.hasCapabilityPublishPosts = enablePublicizeSupport
        if (isJetpackConnected) {
            if (enablePublicizeSupport) activeModules = SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE
        } else {
            setIsPublicizePermanentlyDisabled(!enablePublicizeSupport)
        }
    }

    private fun SiteModel.setShareButtonsSupport(enableShareButtonsSupport: Boolean) {
        hasCapabilityManageOptions = enableShareButtonsSupport
        if (isJetpackConnected) {
            if (enableShareButtonsSupport) activeModules = SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS
        }
    }
}
