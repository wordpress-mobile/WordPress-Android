package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat.Fixable
import java.lang.reflect.Type

class FixableDeserializer : JsonDeserializer<Fixable?> {
    @Suppress("SwallowedException")
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Fixable? {
        /**
         * If a threat is not fixable, fixable is returned as a boolean (false).
         * It is set to null in this case to avoid json parsing issue.
         */
        return if (context != null && json != null && json.isJsonObject) {
            val fixableType = object : TypeToken<Fixable>() { }.type
            val result: Fixable?
            result = try {
                context.deserialize(json, fixableType)
            } catch (e: JsonSyntaxException) {
                null
            }
            result
        } else {
            null
        }
    }
}
