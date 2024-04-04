package org.wordpress.android.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EnumWithFallbackValueTypeAdapterFactoryTest {
    private lateinit var gsonWithFactory: Gson

    @Before
    fun setUp() {
        gsonWithFactory = GsonBuilder()
            .registerTypeAdapterFactory(EnumWithFallbackValueTypeAdapterFactory())
            .create()
    }

    @Test
    fun `deserialize with existing values`() {
        val jsonTemplate = "\"%s\""

        TestEnum.values().forEach { value ->
            val json = jsonTemplate.format(value.name)
            val result = gsonWithFactory.fromJson(json, TestEnum::class.java)
            assertEquals(value, result)
        }
    }

    @Test
    fun `deserialize with fallback value`() {
        val json = "\"NOT_KNOWN\""
        val result = gsonWithFactory.fromJson(json, TestEnum::class.java)
        assertEquals(TestEnum.UNKNOWN, result)
    }
}

private enum class TestEnum {
    @FallbackValue
    UNKNOWN,
    KNOWN,
}
