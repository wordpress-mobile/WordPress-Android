package org.wordpress.android.fluxc.network.discovery

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DiscoveryUtilsTest {
    @Test
    fun `stripKnownPaths removes wp-json path`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-json"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes wp-json with trailing slash`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-json/"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes wp-json with subpath`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-json/wp/v2"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes wp-admin path`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-admin/options.php"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes wp-login path`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-login.php"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes wp-content path`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/wp-content/uploads"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes xmlrpc path`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/xmlrpc.php?rsd"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths removes trailing slashes`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths preserves clean URL`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com"
        )
        assertEquals("https://example.com", result)
    }

    @Test
    fun `stripKnownPaths preserves subdirectory install URL`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/blog"
        )
        assertEquals("https://example.com/blog", result)
    }

    @Test
    fun `stripKnownPaths removes wp-json from subdirectory install`() {
        val result = DiscoveryUtils.stripKnownPaths(
            "https://example.com/blog/wp-json/"
        )
        assertEquals("https://example.com/blog", result)
    }
}
