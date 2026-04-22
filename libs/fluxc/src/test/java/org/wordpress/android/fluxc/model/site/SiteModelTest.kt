package org.wordpress.android.fluxc.model.site

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.site.SiteUtils
import kotlin.test.assertFalse
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
