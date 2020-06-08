package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeBuilder
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeElementBuilder
import java.lang.reflect.Type

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
    @JsonAdapter(EditorThemeElementListSerializer::class)
    @SerializedName("editor-color-palette")
    val colors: List<EditorThemeElement>?,
    @JsonAdapter(EditorThemeElementListSerializer::class)
    @SerializedName("editor-gradient-presets")
    val gradients: List<EditorThemeElement>?
) {
    private val MAP_KEY_ELEMENT_COLORS = "colors"
    private val MAP_KEY_ELEMENT_GRADIENTS = "gradients"
    fun toBundle(): Bundle {
        val bundle = Bundle()

        colors?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList(MAP_KEY_ELEMENT_COLORS, ArrayList<Bundle>(it))
        }

        gradients?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList(MAP_KEY_ELEMENT_GRADIENTS, ArrayList<Bundle>(it))
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
    private val MAP_KEY_ELEMENT_DISPLAY_NAME = "name"
    private val MAP_KEY_ELEMENT_SLUG = "slug"
    private val MAP_KEY_ELEMENT_COLOR_VALUE = "color"
    private val MAP_KEY_ELEMENT_GRADIENT_VALUE = "gradient"

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(MAP_KEY_ELEMENT_DISPLAY_NAME, name)
        bundle.putString(MAP_KEY_ELEMENT_SLUG, slug)
        if (color != null) {
            bundle.putString(MAP_KEY_ELEMENT_COLOR_VALUE, color)
        }
        if (gradient != null) {
            bundle.putString(MAP_KEY_ELEMENT_GRADIENT_VALUE, gradient)
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

class EditorThemeElementListSerializer : JsonDeserializer<List<EditorThemeElement>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<EditorThemeElement>? {
        if (context != null && json != null && json.isJsonArray()) {
            val editorThemeElementListType = object : TypeToken<List<EditorThemeElement>>() { }.getType()
            return context.deserialize(json, editorThemeElementListType)
        } else {
            return null
        }
    }
}
