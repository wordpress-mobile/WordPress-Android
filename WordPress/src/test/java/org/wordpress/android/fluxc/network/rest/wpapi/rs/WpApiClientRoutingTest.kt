package org.wordpress.android.fluxc.network.rest.wpapi.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.extensions.shouldUseWpComProxy

class WpApiClientRoutingTest {
    @Test
    fun `WP_com Simple always uses proxy`() {
        assertThat(simpleSite().shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `WP_com Simple with app password still uses proxy`() {
        assertThat(simpleSite().withAppPassword().shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Atomic without app password uses proxy`() {
        assertThat(atomicSite().shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Atomic with app password talks directly`() {
        assertThat(atomicSite().withAppPassword().shouldUseWpComProxy()).isFalse()
    }

    @Test
    fun `Jetpack REST site without app password uses proxy`() {
        assertThat(jetpackRestSite().shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Jetpack REST site with app password talks directly`() {
        assertThat(jetpackRestSite().withAppPassword().shouldUseWpComProxy()).isFalse()
    }

    @Test
    fun `Jetpack XMLRPC site without app password uses proxy`() {
        assertThat(jetpackXmlRpcSite().shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Jetpack XMLRPC site with app password talks directly`() {
        assertThat(jetpackXmlRpcSite().withAppPassword().shouldUseWpComProxy()).isFalse()
    }

    @Test
    fun `self-hosted without app password talks directly`() {
        assertThat(selfHostedSite().shouldUseWpComProxy()).isFalse()
    }

    @Test
    fun `self-hosted with app password talks directly`() {
        assertThat(selfHostedSite().withAppPassword().shouldUseWpComProxy()).isFalse()
    }

    @Test
    fun `Atomic with empty-string app password uses proxy`() {
        val site = atomicSite().apply {
            apiRestUsernamePlain = ""
            apiRestPasswordPlain = ""
        }
        assertThat(site.shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Atomic site without Jetpack connected still uses proxy without app password`() {
        val site = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        assertThat(site.shouldUseWpComProxy()).isTrue()
    }

    @Test
    fun `Jetpack connected non-atomic still uses proxy without app password`() {
        val site = SiteModel().apply {
            setIsWPCom(false)
            setIsWPComAtomic(false)
            setIsJetpackConnected(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        assertThat(site.shouldUseWpComProxy()).isTrue()
    }

    private fun simpleSite() = SiteModel().apply {
        setIsWPCom(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private fun atomicSite() = SiteModel().apply {
        setIsWPCom(true)
        setIsWPComAtomic(true)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private fun jetpackRestSite() = SiteModel().apply {
        setIsWPCom(false)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private fun jetpackXmlRpcSite() = SiteModel().apply {
        setIsWPCom(false)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        origin = SiteModel.ORIGIN_XMLRPC
    }

    private fun selfHostedSite() = SiteModel().apply {
        setIsWPCom(false)
        setIsJetpackInstalled(false)
        setIsJetpackConnected(false)
        origin = SiteModel.ORIGIN_XMLRPC
    }

    private fun SiteModel.withAppPassword(): SiteModel = apply {
        apiRestUsernamePlain = "user"
        apiRestPasswordPlain = "pass"
    }
}
