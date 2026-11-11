package org.wordpress.android.ui.newstats.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class StatsUpgradeUrlTest {
    @Test
    fun `given a simple wpcom site, when statsUpgradeUrl, then wpcom pricing is returned`() {
        val site = SiteModel().apply { setIsWPCom(true) }

        assertThat(site.statsUpgradeUrl()).isEqualTo("https://wordpress.com/pricing/")
    }

    @Test
    fun `given an atomic site, when statsUpgradeUrl, then wpcom pricing is returned`() {
        val site = SiteModel().apply { setIsWPComAtomic(true) }

        assertThat(site.statsUpgradeUrl()).isEqualTo("https://wordpress.com/pricing/")
    }

    @Test
    fun `given a jetpack site, when statsUpgradeUrl, then jetpack pricing is returned`() {
        val site = SiteModel().apply { setIsJetpackConnected(true) }

        assertThat(site.statsUpgradeUrl()).isEqualTo("https://cloud.jetpack.com/pricing")
    }
}
