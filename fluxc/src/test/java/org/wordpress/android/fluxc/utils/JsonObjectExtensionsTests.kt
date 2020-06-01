package org.wordpress.android.fluxc.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Test
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val sampleJson =
"""
{
    "string": "Some string",
    "escaped_string": "\\ \" '",
    "number": 37,
    "nullstring": null,
    "object": {
        "name": "Object name"
    }
}
"""

class JsonObjectExtensionsTests {
    private val jsonObject by lazy {
        JsonParser().parse(sampleJson).asJsonObject
    }

    @Test
    fun testNullGetters() {
        val nullJsonObject: JsonObject? = null
        assertNull(nullJsonObject.getString(""))
        assertNull(nullJsonObject.getJsonObject(""))
        assertEquals(0, nullJsonObject.getInt(""))
        assertEquals(3, nullJsonObject.getInt("", 3))
    }

    @Test
    fun testGetString() {
        assertEquals("Some string", jsonObject.getString("string"))
        assertEquals("\\ \" '", jsonObject.getString("escaped_string", true))
        assertNull(jsonObject.getString("doesn't exist"))
        assertNull(jsonObject.getString("nullstring"))
    }

    @Test
    fun testGetInt() {
        assertEquals(37, jsonObject.getInt("number"))
        assertEquals(0, jsonObject.getInt("doesn't exist"))
        assertEquals(99, jsonObject.getInt("doesn't exist", 99))
    }

    @Test
    fun testGetObject() {
        val obj = jsonObject.getJsonObject("object")
        assertNotNull(obj)
        assertEquals("Object name", obj.getString("name"))
        assertNull(obj.getJsonObject("doesn't exist"))
    }
}
