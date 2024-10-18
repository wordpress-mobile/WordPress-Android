package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.FixThreatsStatusResponse.FixThreatStatus
import java.lang.reflect.Type

class FixThreatsStatusDeserializer : JsonDeserializer<List<FixThreatStatus>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = if (context != null && json != null && json.isJsonObject) {
        deserializeJson(json)
    } else {
        null
    }

    /**
     * Input: { "39154018": {"status": "fixed" }} / { "39154018": {"error": "not_found" }}
     * Output: [{ "id": 39154018, "status": "fixed" }] /  [{ "id": 39154018, "error": "not_found" }]
     */
    private fun deserializeJson(json: JsonElement) = mutableListOf<FixThreatStatus>().apply {
        val inputJsonObject = json.asJsonObject
        inputJsonObject.keySet().iterator().forEach { key ->
            getFixThreatStatus(key, inputJsonObject)?.let { add(it) }
        }
    }.toList()

    @Suppress("SwallowedException")
    private fun getFixThreatStatus(key: String, inputJsonObject: JsonObject) =
        inputJsonObject.get(key)?.takeIf { it.isJsonObject }?.asJsonObject?.let { threat ->
            try {
                FixThreatStatus(
                    id = key.toLong(),
                    status = threat.get(STATUS)?.asString,
                    error = threat.get(ERROR)?.asString
                )
            } catch (ex: ClassCastException) {
                null
            } catch (ex: IllegalStateException) {
                null
            } catch (ex: NumberFormatException) {
                null
            }
        }

    companion object {
        private const val STATUS = "status"
        private const val ERROR = "error"
    }
}
