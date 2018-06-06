package org.wordpress.android.fluxc.network.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject?.getString(property: String): String? = checkAndGet(property)?.asString
fun JsonObject?.getInt(property: String, defaultValue: Int = 0): Int = checkAndGet(property)?.asInt ?: defaultValue
fun JsonObject?.getJsonObject(property: String): JsonObject? {
    val obj = checkAndGet(property)
    return if (obj?.isJsonObject == true) obj.asJsonObject else null
}

private fun JsonObject?.checkAndGet(property: String): JsonElement? = if (this?.has(property) == true) {
    this.get(property)
} else null
