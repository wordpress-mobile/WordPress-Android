package org.wordpress.android.util.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class SiteModelExtensionsKtTest {
    @Test
    fun `given a WordPress-com site, then it can use the Jetpack app`() {
        val site = SiteModel().apply { setIsWPCom(true) }

        assertThat(site.canUseJetpackApp()).isTrue
    }

    @Test
    fun `given a self-hosted site with Jetpack installed, then it can use the Jetpack app`() {
        val site = SiteModel().apply { setIsJetpackInstalled(true) }

        assertThat(site.canUseJetpackApp()).isTrue
    }

    @Test
    fun `given a self-hosted site connected to Jetpack, then it can use the Jetpack app`() {
        val site = SiteModel().apply { setIsJetpackConnected(true) }

        assertThat(site.canUseJetpackApp()).isTrue
    }

    @Test
    fun `given a Jetpack-CP connected site, then it can use the Jetpack app`() {
        val site = SiteModel().apply { setIsJetpackCPConnected(true) }

        assertThat(site.canUseJetpackApp()).isTrue
    }

    @Test
    fun `given a self-hosted site without Jetpack, then it cannot use the Jetpack app`() {
        assertThat(SiteModel().canUseJetpackApp()).isFalse
    }

    @Test
    fun `given no site, then it cannot use the Jetpack app`() {
        assertThat(null.canUseJetpackApp()).isFalse
    }
}
