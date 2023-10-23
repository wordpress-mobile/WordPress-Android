package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.util.Locale

internal class BooleanTypeAdapter : JsonDeserializer<Boolean?> {
    @Suppress("VariableNaming") private val TRUE_STRINGS: Set<String> = HashSet(listOf("true", "1", "yes"))

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Boolean {
        val jsonPrimitive = json.asJsonPrimitive
        return when {
            jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
            jsonPrimitive.isNumber -> jsonPrimitive.asNumber.toInt() == 1
            jsonPrimitive.isString -> TRUE_STRINGS.contains(jsonPrimitive.asString.toLowerCase(
                Locale.getDefault()
            ))
            else -> false
        }
    }
}
