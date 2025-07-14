package org.wordpress.android.ui.accounts.login

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiRootUrlCacheTest {
    private lateinit var apiRootUrlCache: ApiRootUrlCache

    @Before
    fun setUp() {
        apiRootUrlCache = ApiRootUrlCache()
    }

    @Test
    fun `put and get returns correct value`() {
        val key = "test-site.com"
        val value = "https://test-site.com/wp-json/"

        apiRootUrlCache.put(key, value)

        assertEquals(value, apiRootUrlCache.get(key))
    }

    @Test
    fun `get returns null for non-existent key`() {
        val result = apiRootUrlCache.get("non-existent-key")

        assertNull(result)
    }

    @Test
    fun `put overwrites existing value`() {
        val key = "test-site.com"
        val originalValue = "https://test-site.com/wp-json/"
        val newValue = "https://test-site.com/api/"

        apiRootUrlCache.put(key, originalValue)
        apiRootUrlCache.put(key, newValue)

        assertEquals(newValue, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache can store multiple entries`() {
        val key1 = "site1.com"
        val value1 = "https://site1.com/wp-json/"
        val key2 = "site2.com"
        val value2 = "https://site2.com/wp-json/"
        val key3 = "site3.com"
        val value3 = "https://site3.com/api/"

        apiRootUrlCache.put(key1, value1)
        apiRootUrlCache.put(key2, value2)
        apiRootUrlCache.put(key3, value3)

        assertEquals(value1, apiRootUrlCache.get(key1))
        assertEquals(value2, apiRootUrlCache.get(key2))
        assertEquals(value3, apiRootUrlCache.get(key3))
    }

    @Test
    fun `cache doesn't handle empty string values`() {
        val key = "empty-site.com"
        val value = ""

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
    fun `cache handles special characters in keys`() {
        val key = "test-site.com/subdir?param=value"
        val value = "https://test-site.com/subdir/wp-json/"

        apiRootUrlCache.put(key, value)

        assertEquals(value, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache handles special characters in values`() {
        val key = "test-site.com"
        val value = "https://test-site.com/wp-json/?auth=token&user=test@email.com"

        apiRootUrlCache.put(key, value)

        assertEquals(value, apiRootUrlCache.get(key))
    }

    @Test
    fun `cache is case sensitive for keys`() {
        val keyLowerCase = "test-site.com"
        val keyUpperCase = "TEST-SITE.COM"
        val value1 = "https://test-site.com/wp-json/"
        val value2 = "https://TEST-SITE.COM/wp-json/"

        apiRootUrlCache.put(keyLowerCase, value1)
        apiRootUrlCache.put(keyUpperCase, value2)

        assertEquals(value1, apiRootUrlCache.get(keyLowerCase))
        assertEquals(value2, apiRootUrlCache.get(keyUpperCase))
    }
}
