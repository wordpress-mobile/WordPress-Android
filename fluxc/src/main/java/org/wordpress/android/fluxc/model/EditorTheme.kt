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
import org.wordpress.android.util.VersionUtils
import java.lang.reflect.Type

private const val GALLERY_V2_WP_VERSION = "5.9"

const val MAP_KEY_ELEMENT_DISPLAY_NAME: String = "name"
const val MAP_KEY_ELEMENT_SLUG: String = "slug"
const val MAP_KEY_ELEMENT_COLORS: String = "colors"
const val MAP_KEY_ELEMENT_GRADIENTS: String = "gradients"
const val MAP_KEY_ELEMENT_STYLES: String = "rawStyles"
const val MAP_KEY_ELEMENT_FEATURES: String = "rawFeatures"
const val MAP_KEY_IS_BLOCK_BASED_THEME: String = "isBlockBasedTheme"
const val MAP_KEY_GALLERY_WITH_IMAGE_BLOCKS: String = "galleryWithImageBlocks"
const val MAP_KEY_QUOTE_BLOCK_V2: String = "quoteBlockV2"
const val MAP_KEY_LIST_BLOCK_V2: String = "listBlockV2"
const val MAP_KEY_HAS_BLOCK_TEMPLATES: String = "hasBlockTemplates"

data class EditorTheme(
    @SerializedName("theme_supports") val themeSupport: EditorThemeSupport,
    val stylesheet: String?,
    val version: String?
) {
    constructor(blockEditorSettings: BlockEditorSettings) : this(
            themeSupport = EditorThemeSupport(
                    blockEditorSettings.colors,
                    blockEditorSettings.gradients,
                    null,
                    blockEditorSettings.styles?.toString(),
                    blockEditorSettings.featuresFiltered?.toString(),
                    blockEditorSettings.isBlockBasedTheme,
                    blockEditorSettings.galleryWithImageBlocks,
                    blockEditorSettings.quoteBlockV2,
                    blockEditorSettings.listBlockV2
            ),
            stylesheet = null,
            version = null
    )

    fun toBuilder(site: SiteModel): EditorThemeBuilder {
        val element = EditorThemeBuilder()
        element.localSiteId = site.id
        element.stylesheet = stylesheet
        element.version = version
        element.rawStyles = themeSupport.rawStyles
        element.rawFeatures = themeSupport.rawFeatures
        element.isBlockBasedTheme = themeSupport.isBlockBasedTheme
        element.galleryWithImageBlocks = themeSupport.galleryWithImageBlocks ?: site.coreSupportsGalleryV2
        element.quoteBlockV2 = themeSupport.quoteBlockV2
        element.listBlockV2 = themeSupport.listBlockV2
        element.hasBlockTemplates = themeSupport.hasBlockTemplates ?: false

        return element
    }

    override fun equals(other: Any?): Boolean {
        if (other == null ||
                other !is EditorTheme ||
                themeSupport != other.themeSupport) return false

        return true
    }
}

data class BlockEditorSettings(
    @SerializedName("__unstableIsBlockBasedTheme") val isBlockBasedTheme: Boolean,
    @SerializedName("__unstableGalleryWithImageBlocks") val galleryWithImageBlocks: Boolean,
    @SerializedName("__experimentalEnableQuoteBlockV2") val quoteBlockV2: Boolean,
    @SerializedName("__experimentalEnableListBlockV2") val listBlockV2: Boolean,
    @SerializedName("__experimentalStyles") val styles: JsonElement?,
    @SerializedName("__experimentalFeatures") val features: JsonElement?,
    @JsonAdapter(EditorThemeElementListSerializer::class) val colors: List<EditorThemeElement>?,
    @JsonAdapter(EditorThemeElementListSerializer::class) val gradients: List<EditorThemeElement>?
) {
    val featuresFiltered: JsonElement?
        get() = features?.removeFontFamilies()

    private fun JsonElement.removeFontFamilies(): JsonElement {
        if (isJsonObject && asJsonObject.has("typography")) {
            val featuresObject = asJsonObject
            val typography = featuresObject.get("typography")
            if (typography.isJsonObject) {
                val typographyObject = typography.asJsonObject
                if (typographyObject.has("fontFamilies")) {
                    typographyObject.remove("fontFamilies")
                    return featuresObject
                }
            }
        }
        return this
    }
}

data class EditorThemeSupport(
    @JsonAdapter(EditorThemeElementListSerializer::class)
    @SerializedName("editor-color-palette")
    val colors: List<EditorThemeElement>?,
    @JsonAdapter(EditorThemeElementListSerializer::class)
    @SerializedName("editor-gradient-presets")
    val gradients: List<EditorThemeElement>?,
    @SerializedName("block-templates")
    val hasBlockTemplates: Boolean?,
    val rawStyles: String?,
    val rawFeatures: String?,
    val isBlockBasedTheme: Boolean,
    val galleryWithImageBlocks: Boolean?,
    val quoteBlockV2: Boolean,
    val listBlockV2: Boolean
) {
    fun toBundle(site: SiteModel): Bundle {
        val bundle = Bundle()

        colors?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList(MAP_KEY_ELEMENT_COLORS, ArrayList<Bundle>(it))
        }

        gradients?.map { it.toBundle() }?.let {
            bundle.putParcelableArrayList(MAP_KEY_ELEMENT_GRADIENTS, ArrayList<Bundle>(it))
        }

        rawStyles?.let {
            bundle.putString(MAP_KEY_ELEMENT_STYLES, it)
        }

        rawFeatures?.let {
            bundle.putString(MAP_KEY_ELEMENT_FEATURES, it)
        }

        bundle.putBoolean(MAP_KEY_IS_BLOCK_BASED_THEME, isBlockBasedTheme)
        bundle.putBoolean(MAP_KEY_GALLERY_WITH_IMAGE_BLOCKS, galleryWithImageBlocks ?: site.coreSupportsGalleryV2)
        bundle.putBoolean(MAP_KEY_QUOTE_BLOCK_V2, quoteBlockV2)
        bundle.putBoolean(MAP_KEY_LIST_BLOCK_V2, listBlockV2)
        bundle.putBoolean(MAP_KEY_HAS_BLOCK_TEMPLATES, hasBlockTemplates ?: false)

        return bundle
    }
    fun isEditorThemeBlockBased(): Boolean = isBlockBasedTheme || (hasBlockTemplates ?: false)
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
    @Suppress("SwallowedException")
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

private val SiteModel.coreSupportsGalleryV2: Boolean
    get() = VersionUtils.checkMinimalVersion(softwareVersion, GALLERY_V2_WP_VERSION)
