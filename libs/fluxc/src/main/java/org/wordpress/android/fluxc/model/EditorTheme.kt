package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.persistence.EditorThemeElementType
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeBuilder
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils.EditorThemeElementBuilder
import java.lang.reflect.Type

const val MAP_KEY_ELEMENT_DISPLAY_NAME: String = "name"
const val MAP_KEY_ELEMENT_SLUG: String = "slug"
const val MAP_KEY_ELEMENT_COLORS: String = "colors"
const val MAP_KEY_ELEMENT_GRADIENTS: String = "gradients"

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
    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(MAP_KEY_ELEMENT_DISPLAY_NAME, name)
        bundle.putString(MAP_KEY_ELEMENT_SLUG, slug)
        if (color != null) {
            bundle.putString(EditorThemeElementType.COLOR.value, color)
        }
        if (gradient != null) {
            bundle.putString(EditorThemeElementType.GRADIENT.value, gradient)
        }
        return bundle
    }

    fun toBuilder(themeId: Int): EditorThemeElementBuilder {
        val isColor = color != null
        val element = EditorThemeElementBuilder()
        element.type = if (isColor) EditorThemeElementType.COLOR.value else EditorThemeElementType.GRADIENT.value
        element.name = name
        element.slug = slug
        element.value = if (isColor) color else gradient
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
            var result: List<EditorThemeElement>?
            try {
                result = context.deserialize(json, editorThemeElementListType)
            } catch (e: JsonSyntaxException) {
                result = null
            }
            return result
        } else {
            return null
        }
    }
}
