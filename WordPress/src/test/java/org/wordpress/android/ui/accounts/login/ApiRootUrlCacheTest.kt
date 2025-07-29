package org.wordpress.android.ui.accounts.login

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.wordpress.android.util.UriUtilsWrapper
import org.wordpress.android.util.UriWrapper
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiRootUrlCacheTest {
    private lateinit var apiRootUrlCache: ApiRootUrlCache

    @Mock
    lateinit var uriUtilsWrapper: UriUtilsWrapper
    @Mock
    @Suppress("DoNotMockDataClass") // This class is intended to be mocked in tests
    lateinit var uriWrapper: UriWrapper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        apiRootUrlCache = ApiRootUrlCache(uriUtilsWrapper)
    }

    @Test
    fun `put and get returns correct value with domain extraction`() {
        val value = "https://test-site.com/wp-json/"

        whenever(uriWrapper.host).doReturn("test-site.com")
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put("test-site.com", value)

        // Should work with full URL
        assertEquals(value, apiRootUrlCache.get("https://test-site.com/path"))
        // Should also work with just domain
        assertEquals(value, apiRootUrlCache.get("test-site.com"))
        // Should work with different protocol
        assertEquals(value, apiRootUrlCache.get("http://test-site.com"))
    }

    @Test
    fun `get returns null for non-existent key`() {
        whenever(uriWrapper.host).doReturn("new-key")
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        val result = apiRootUrlCache.get("non-existent-key")

        assertNull(result)
    }

    @Test
    fun `put overwrites existing value`() {
        val key = "test-site.com"
        val originalValue = "https://test-site.com/wp-json/"
        val newValue = "https://test-site.com/api/"

        whenever(uriWrapper.host).doReturn(key)
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put(key, originalValue)
        apiRootUrlCache.put(key, newValue)

        assertEquals(newValue, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache doesn't handle empty string values`() {
        val key = "empty-site.com"
        val value = ""

        whenever(uriWrapper.host).doReturn(key)
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put(key, value)

        assertEquals(null, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache doesn't handle empty string keys`() {
        val key = ""
        val value = "https://site1.com/wp-json/"

        apiRootUrlCache.put(key, value)

        assertEquals(null, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache handles special characters in values`() {
        val key = "test-site.com"
        val value = "https://test-site.com/wp-json/?auth=token&user=test@email.com"

        whenever(uriWrapper.host).doReturn(key)
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put(key, value)

        assertEquals(value, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache is case insensitive for domains`() {
        val keyLowerCase = "https://test-site.com"
        val keyUpperCase = "https://TEST-SITE.COM"
        val value = "https://test-site.com/wp-json/"

        whenever(uriWrapper.host).doReturn(keyUpperCase)
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put(keyLowerCase, value)

        // Both keys should return the same value due to case-insensitive domain extraction
        assertEquals(value, apiRootUrlCache.get(keyLowerCase))
        assertEquals(value, apiRootUrlCache.get(keyUpperCase))
        assertEquals(value, apiRootUrlCache.get("TEST-SITE.COM"))
        assertEquals(value, apiRootUrlCache.get("test-site.com"))
    }

    @Test
    fun `cache removes www prefix from domains`() {
        val urlWithWww = "http://www.mysite.wordpress.com/path"
        val value = "https://mysite.wordpress.com/wp-json/"

        whenever(uriWrapper.host).doReturn("http://www.mysite.wordpress.com")
        whenever(uriUtilsWrapper.parse(any())).doReturn(uriWrapper)

        apiRootUrlCache.put(urlWithWww, value)

        // Should be accessible with or without www
        assertEquals(value, apiRootUrlCache.get("mysite.wordpress.com"))
        assertEquals(value, apiRootUrlCache.get("www.mysite.wordpress.com"))
        assertEquals(value, apiRootUrlCache.get("http://mysite.wordpress.com"))
    }
}
