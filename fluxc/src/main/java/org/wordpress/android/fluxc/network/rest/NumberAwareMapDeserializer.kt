package org.wordpress.android.fluxc.network.rest

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.internal.LazilyParsedNumber
import java.lang.reflect.Type

/**
 * A custom deserializer for JSON objects into Maps that is aware of number types.
 * This deserializer ensures that numbers are correctly parsed and converted to the appropriate
 * type (e.g., integer, long, double) based on their value.
 */
class NumberAwareMapDeserializer : JsonDeserializer<Map<*, *>> {
    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Map<*, *> {
        val map: MutableMap<Any, Any?> = HashMap()
        try {
            val jsonObject = json.asJsonObject
            for ((key, value) in jsonObject.entrySet()) {
                if (key !is String) {
                    throw JsonParseException("Invalid key type: ${key::class.java}")
                }
                if (value.isJsonPrimitive) {
                    val primitive = value.asJsonPrimitive
                    if (primitive.isNumber) {
                        map[key] = convertNumber(primitive.asNumber)
                    } else if (primitive.isBoolean) {
                        map[key] = primitive.asBoolean
                    } else {
                        map[key] = primitive.asString
                    }
                } else if (value.isJsonObject) {
                    map[key] = context.deserialize<Map<*, *>>(value, Map::class.java)
                } else if (value.isJsonArray) {
                    map[key] = value.asJsonArray.map {
                        if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) {
                            convertNumber(it.asJsonPrimitive.asNumber)
                        } else {
                            context.deserialize<Any>(it, Any::class.java)
                        }
                    }
                } else {
                    map[key] = null
                }
            }
        } catch (e: Exception) {
            throw JsonParseException("Error deserializing JSON to Map", e)
        }
        return map
    }

    private fun convertNumber(numberValue: Number): Number {
        return when {
            numberValue is LazilyParsedNumber -> {
                val numberAsString = numberValue.toString()
                if (numberAsString.contains('.')) {
                    val doubleValue = numberAsString.toDouble()
                    if (doubleValue % 1 == 0.0) {
                        doubleValue.toLong()
                    } else {
                        doubleValue
                    }
                } else {
                    val longValue = numberAsString.toLong()
                    if (longValue in Integer.MIN_VALUE..Integer.MAX_VALUE) {
                        longValue.toInt()
                    } else {
                        longValue
                    }
                }
            }
            numberValue is Long && numberValue in Integer.MIN_VALUE..Integer.MAX_VALUE -> numberValue.toInt()
            numberValue is Double && numberValue % 1 == 0.0 -> numberValue.toLong()
            else -> numberValue
        }
    }
}
