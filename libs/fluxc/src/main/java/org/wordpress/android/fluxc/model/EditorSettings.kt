package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class EditorSettings(
    val settings: JsonObject
) {
    fun toJsonString(): String {
        return settings.toString()
    }
}
