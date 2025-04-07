package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class EditorSettings(
    @SerializedName("colors") val colors: List<EditorThemeElement>? = null,
    @SerializedName("gradients") val gradients: List<EditorThemeElement>? = null,
    @SerializedName("styles") val styles: String? = null,
    @SerializedName("features") val features: String? = null,
    @SerializedName("isBlockBasedTheme") val isBlockBasedTheme: Boolean = false,
    @SerializedName("galleryWithImageBlocks") val galleryWithImageBlocks: Boolean = false,
    @SerializedName("quoteBlockV2") val quoteBlockV2: Boolean = false,
    @SerializedName("listBlockV2") val listBlockV2: Boolean = false,
    @SerializedName("hasBlockTemplates") val hasBlockTemplates: Boolean = false,
    val rawSettings: JsonObject? = null
) {
    fun toBundle(): Bundle {
        val bundle = Bundle()

        colors?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList("colors", ArrayList(it))
        }

        gradients?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList("gradients", ArrayList(it))
        }

        styles?.let { bundle.putString("styles", it) }
        features?.let { bundle.putString("features", it) }
        bundle.putBoolean("isBlockBasedTheme", isBlockBasedTheme)
        bundle.putBoolean("galleryWithImageBlocks", galleryWithImageBlocks)
        bundle.putBoolean("quoteBlockV2", quoteBlockV2)
        bundle.putBoolean("listBlockV2", listBlockV2)
        bundle.putBoolean("hasBlockTemplates", hasBlockTemplates)

        rawSettings?.let { json ->
            json.entrySet().forEach { entry ->
                when (val value = entry.value) {
                    is com.google.gson.JsonPrimitive -> {
                        if (value.isBoolean) {
                            bundle.putBoolean(entry.key, value.asBoolean)
                        } else if (value.isNumber) {
                            bundle.putDouble(entry.key, value.asDouble)
                        } else if (value.isString) {
                            bundle.putString(entry.key, value.asString)
                        }
                    }
                    is com.google.gson.JsonArray -> {
                        // Handle arrays if needed
                    }
                    is com.google.gson.JsonObject -> {
                        // Handle nested objects if needed
                    }
                }
            }
        }

        return bundle
    }
}
