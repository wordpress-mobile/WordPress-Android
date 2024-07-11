package org.wordpress.android.fluxc.network.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.apache.commons.text.StringEscapeUtils

fun JsonObject?.getString(property: String, unescapeHtml4: Boolean = false): String? {
    val str = checkAndGet(property)?.asString
    return if (unescapeHtml4) {
        StringEscapeUtils.unescapeHtml4(str)
    } else str
}

fun JsonObject?.getBoolean(property: String, defaultValue: Boolean = false) =
        checkAndGet(property)?.asBoolean ?: defaultValue

fun JsonObject?.getInt(property: String, defaultValue: Int = 0): Int = checkAndGet(property)?.asInt ?: defaultValue

fun JsonObject?.getLong(property: String, defaultValue: Long = 0L) =
        checkAndGet(property)?.asLong ?: defaultValue

fun JsonObject?.getJsonObject(property: String): JsonObject? {
    val obj = checkAndGet(property)
    return if (obj?.isJsonObject == true) obj.asJsonObject else null
}

private fun JsonObject?.checkAndGet(property: String): JsonElement? {
    return if (this?.has(property) == true) {
        val jsonElement = this.get(property)
        if (jsonElement.isJsonNull) {
            null
        } else {
            jsonElement
        }
    } else null
}
