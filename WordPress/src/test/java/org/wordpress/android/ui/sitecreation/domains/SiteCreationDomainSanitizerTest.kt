package org.wordpress.android.ui.sitecreation.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SiteCreationDomainSanitizerTest {
    private val domainSanitizer = SiteCreationDomainSanitizer()

    @Test
    fun `Verify everything after the first period is removed`() {
        val result = domainSanitizer.sanitizeDomainQuery("test.wordpress.com")
        assertThat(result.contains("wordpress.com")).isFalse
    }

    @Test
    fun `Verify that a word doesn't break the sanitizer and its value is returned`() {
        val result = domainSanitizer.sanitizeDomainQuery("test")
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun `Remove https if its present`() {
        val result = domainSanitizer.sanitizeDomainQuery("https://test.wordpress.com")
        assertThat(result.contains("https://")).isFalse
    }

    @Test
    fun `Remove http if its present`() {
        val result = domainSanitizer.sanitizeDomainQuery("http://test.wordpress.com")
        assertThat(result.contains("http://")).isFalse
    }

    @Test
    fun `Remove all characters that are not alphanumeric`() {
        val result = domainSanitizer.sanitizeDomainQuery("test_this-site.wordpress.com")
        assertThat(result).isEqualTo("testthissite")
    }

    @Test
    fun `Get first domain part`() {
        val result = domainSanitizer.getName("https://test_this-site.wordpress.com")
        assertThat(result).isEqualTo("test_this-site")
    }

    @Test
    fun `Get second domain part`() {
        val result = domainSanitizer.getDomain("https://test_this-site.wordpress.com")
        assertThat(result).isEqualTo(".wordpress.com")
    }
}
