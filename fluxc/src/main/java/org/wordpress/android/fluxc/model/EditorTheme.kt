package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class EditorTheme(
    @SerializedName("theme_supports") val themeSupport: EditorThemeSupport,
    val stylesheet: String?,
    val version: String?
)

data class EditorThemeSupport(
    @SerializedName("editor-color-palette") val colors: JsonArray?,
    @SerializedName("editor-gradient-presets") val gradients: JsonArray?
)