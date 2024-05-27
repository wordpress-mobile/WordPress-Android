package org.wordpress.android.fluxc.network.rest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberAwareMapDeserializerTest {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Map::class.java, NumberAwareMapDeserializer())
        .create()

    @Test
    fun testDeserializeSimpleMap() {
        val json = """{"key1": "value1", "key2": 2, "key3": true}"""
        val result: Map<*, *> = gson.fromJson(json, Map::class.java)

        assertEquals("value1", result["key1"])
        assertEquals(2, result["key2"])
        assertEquals(true, result["key3"])
    }

    @Test
    fun testDeserializeNestedMap() {
        val json = """{"key1": {"nestedKey1": "nestedValue1", "nestedKey2": 5}}"""
        val result: Map<*, *> = gson.fromJson(json, Map::class.java)

        val nestedMap = result["key1"] as Map<*, *>
        assertEquals("nestedValue1", nestedMap["nestedKey1"])
        assertEquals(5, nestedMap["nestedKey2"])
    }

    @Test
    fun testDeserializeArray() {
        val json = """{"key1": [1, 2, 3], "key2": ["a", "b", "c"]}"""
        val result: Map<*, *> = gson.fromJson(json, Map::class.java)

        val array1 = result["key1"] as List<*>
        val array2 = result["key2"] as List<*>
        assertArrayEquals(arrayOf(1, 2, 3), array1.toTypedArray())
        assertArrayEquals(arrayOf("a", "b", "c"), array2.toTypedArray())
    }

    @Test
    fun testDeserializeNumbers() {
        val json = """{"intKey": 2147483647, "longKey": 2147483648, "doubleKey": 1.5, "wholeDoubleKey": 3.0}"""
        val result: Map<*, *> = gson.fromJson(json, Map::class.java)

        assertEquals(2147483647, result["intKey"])
        assertEquals(2147483648L, result["longKey"])
        assertEquals(1.5, result["doubleKey"])
        assertEquals(3L, result["wholeDoubleKey"])
    }

    @Test(expected = JsonParseException::class)
    fun testInvalidKeyType() {
        val json = """{"key1": "value1", "key2": 2, "key3"}""" // Malformed JSON
        gson.fromJson(json, Map::class.java)
    }

    @Test(expected = JsonParseException::class)
    fun testInvalidJson() {
        val json = """{"key1": "value1", "key2":}""" // Malformed JSON
        gson.fromJson(json, Map::class.java)
    }

    @Test
    fun testNullValue() {
        val json = """{"key1": null}"""
        val result: Map<*, *> = gson.fromJson(json, Map::class.java)

        assertNull(result["key1"])
    }
}
