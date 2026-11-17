package org.wordpress.android.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class WPWebViewActivitySiteLoginUrlTest {
    @Test
    fun `the stored login URL is used when the site has one`() {
        val site = SiteModel().apply {
            loginUrl = "https://example.com/login"
            adminUrl = "https://example.com/wp/wp-admin/"
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/login")
    }

    @Test
    fun `the admin URL gives the login URL when the site has no stored one`() {
        val site = SiteModel().apply {
            adminUrl = "https://example.com/wp-admin/"
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp-login.php")
    }

    @Test
    fun `the admin URL keeps the directory WordPress is installed in`() {
        val site = SiteModel().apply {
            adminUrl = "https://example.com/wp/wp-admin/"
            // The site's address is the home URL, which is the subdirectory install's whole point
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp/wp-login.php")
    }

    @Test
    fun `the admin URL is used without its trailing slash`() {
        val site = SiteModel().apply {
            // A subdirectory install, so the result differs from the site's address
            adminUrl = "https://example.com/wp/wp-admin"
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp/wp-login.php")
    }

    @Test
    fun `an empty stored login URL falls through to the admin URL`() {
        val site = SiteModel().apply {
            // The XML-RPC site options store an empty string when they carry no login URL
            loginUrl = ""
            adminUrl = "https://example.com/wp/wp-admin/"
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp/wp-login.php")
    }

    @Test
    fun `the admin URL is used even when its host differs from the site's address`() {
        val site = SiteModel().apply {
            // The admin URL comes from the site's own Application Password authorization endpoint, so a
            // mapped domain or a WP_SITEURL on another host puts the login page there
            adminUrl = "https://admin.example.com/wp-admin/"
            url = "https://www.example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://admin.example.com/wp-login.php")
    }

    @Test
    fun `the site's address is the fallback when there is no usable admin URL`() {
        val site = SiteModel().apply {
            adminUrl = "https://example.com/dashboard/"
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp-login.php")
    }

    @Test
    fun `the site's address is the fallback when the admin URL is empty`() {
        val site = SiteModel().apply {
            url = "https://example.com"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp-login.php")
    }

    @Test
    fun `the XML-RPC URL is the last resort`() {
        val site = SiteModel().apply {
            xmlRpcUrl = "https://example.com/xmlrpc.php"
        }

        assertThat(WPWebViewActivity.getSiteLoginUrl(site)).isEqualTo("https://example.com/wp-login.php")
    }
}
