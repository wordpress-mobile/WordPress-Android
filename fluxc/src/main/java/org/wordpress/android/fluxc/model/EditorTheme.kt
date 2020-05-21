package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.annotations.SerializedName

data class EditorTheme(
    @SerializedName("theme_supports") val themeSupport: EditorThemeSupport,
    val stylesheet: String?,
    val version: String?
)

data class EditorThemeSupport(
    @SerializedName("editor-color-palette") val colors: ArrayList<EditorThemeElement>?,
    @SerializedName("editor-gradient-presets") val gradients: ArrayList<EditorThemeElement>?
) {
    fun toBundle(): Bundle {
        val bundle = Bundle()
        colors?.map { it.toBundle() }.let {
            bundle.putParcelableArrayList("colors", ArrayList<Bundle>(it))
        }

        gradients?.map { it.toBundle() }.let {
            bundle.putParcelableArrayList("gradients", ArrayList<Bundle>(it))
        }

        return bundle
    }
}

data class EditorThemeElement(
    val name: String?,
    val slug: String?,
    val color: String?,
    val gradient: String?
) {
    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("name", name)
        bundle.putString("slug", slug)
        if (color != null) {
            bundle.putString("color", color)
        }
        if (gradient != null) {
            bundle.putString("gradient", gradient)
        }
        return bundle
    }
}
