package org.wordpress.android.ui.sitecreation.domains

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.junit.Test

class SiteCreationDomainSanitizerTest {
    private val domainSanitizer = SiteCreationDomainSanitizer()

    @Test
    fun `Verify everything after the first period is removed`() {
        val result = domainSanitizer.sanitizeDomainQuery("test.wordpress.com")
        assertFalse(result.contains("wordpress.com"))
    }

    @Test
    fun `Verify that a word doesn't break the sanitizer and its value is returned`() {
        val result = domainSanitizer.sanitizeDomainQuery("test")
        assertEquals(result, "test")
    }

    @Test
    fun `Remove https if its present`() {
        val result = domainSanitizer.sanitizeDomainQuery("https://test.wordpress.com")
        assertFalse(result.contains("https://"))
    }

    @Test
    fun `Remove http if its present`() {
        val result = domainSanitizer.sanitizeDomainQuery("http://test.wordpress.com")
        assertFalse(result.contains("http://"))
    }

    @Test
    fun `Remove all characters that are not alphanumeric`() {
        val result = domainSanitizer.sanitizeDomainQuery("test_this-site.wordpress.com")
        assertEquals(result, "testthissite")
    }
}
