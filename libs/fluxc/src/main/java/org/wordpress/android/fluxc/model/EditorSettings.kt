package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONObject

data class EditorSettings(
    val settings: JsonObject
) {
    companion object {
        fun fromJsonString(jsonString: String): EditorSettings {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            return EditorSettings(jsonObject)
        }
    }

    fun toJsonString(): String {
        return settings.toString()
    }

    fun toJSONObject(): JSONObject {
        return JSONObject(settings.toString())
    }
}
