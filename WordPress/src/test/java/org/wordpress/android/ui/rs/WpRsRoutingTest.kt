package org.wordpress.android.ui.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.config.WpComWpRsFeatureConfig

class WpRsRoutingTest {
    private val wpComWpRsFeatureConfig: WpComWpRsFeatureConfig = mock()
    private val wpRsRouting = WpRsRouting(wpComWpRsFeatureConfig)

    @Test
    fun `returns false for a null site`() {
        assertThat(wpRsRouting.canUseWpRs(null)).isFalse()
    }

    @Test
    fun `returns false for a self-hosted site with no application password`() {
        assertThat(wpRsRouting.canUseWpRs(SiteModel())).isFalse()
    }

    @Test
    fun `returns false when only one half of the credentials is stored`() {
        val site = SiteModel()
        site.apiRestUsernamePlain = "user"

        assertThat(wpRsRouting.canUseWpRs(site)).isFalse()

        site.apiRestUsernamePlain = null
        site.apiRestPasswordPlain = "password"

        assertThat(wpRsRouting.canUseWpRs(site)).isFalse()
    }

    @Test
    fun `an application password is enough on its own, whatever the flag says`() {
        whenever(wpComWpRsFeatureConfig.isEnabled()).thenReturn(false)

        assertThat(wpRsRouting.canUseWpRs(appPasswordSite())).isTrue()
    }

    @Test
    fun `a WPCom site stays on the legacy screens while the flag is off`() {
        whenever(wpComWpRsFeatureConfig.isEnabled()).thenReturn(false)

        assertThat(wpRsRouting.canUseWpRs(wpComSite())).isFalse()
    }

    @Test
    fun `a WPCom site gets the rs screens once the flag is on`() {
        whenever(wpComWpRsFeatureConfig.isEnabled()).thenReturn(true)

        assertThat(wpRsRouting.canUseWpRs(wpComSite())).isTrue()
    }

    @Test
    fun `a Jetpack site reached over the WPCom REST API follows the flag too`() {
        whenever(wpComWpRsFeatureConfig.isEnabled()).thenReturn(true)

        assertThat(wpRsRouting.canUseWpRs(jetpackSite())).isTrue()
    }

    /** No application password, no WP.com routing — the flag can't rescue an XML-RPC-only site. */
    @Test
    fun `the flag does not admit a site the rs data layer cannot reach`() {
        whenever(wpComWpRsFeatureConfig.isEnabled()).thenReturn(true)

        val site = SiteModel()
        site.origin = SiteModel.ORIGIN_XMLRPC

        assertThat(wpRsRouting.canUseWpRs(site)).isFalse()
    }

    private fun appPasswordSite() = SiteModel().apply {
        apiRestUsernamePlain = "user"
        apiRestPasswordPlain = "password"
    }

    private fun wpComSite() = SiteModel().apply {
        setIsWPCom(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private fun jetpackSite() = SiteModel().apply {
        setIsJetpackConnected(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }
}
