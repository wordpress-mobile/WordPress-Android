package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeBuilder
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeElementBuilder

data class EditorTheme(
    @SerializedName("theme_supports") val themeSupport: EditorThemeSupport,
    val stylesheet: String?,
    val version: String?
) {
    fun toBuilder(siteId: Int): EditorThemeBuilder {
        val element = EditorThemeBuilder()
        element.localSiteId = siteId
        element.stylesheet = stylesheet
        element.version = version

        return element
    }

    override fun equals(other: Any?): Boolean {
        if (other == null ||
                other !is EditorTheme ||
                stylesheet != other.stylesheet ||
                version != other.version) return false

        return true
    }
}

data class EditorThemeSupport(
    @SerializedName("editor-color-palette") val colors: List<EditorThemeElement>?,
    @SerializedName("editor-gradient-presets") val gradients: List<EditorThemeElement>?
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

    fun toBuilder(themeId: Int): EditorThemeElementBuilder {
        val element = EditorThemeElementBuilder()
        element.isColor = color != null
        element.name = name
        element.slug = slug
        element.value = if (element.isColor) color else gradient
        element.themeId = themeId

        return element
    }
}
