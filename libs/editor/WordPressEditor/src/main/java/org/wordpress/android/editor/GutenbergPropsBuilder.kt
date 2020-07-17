package org.wordpress.android.editor

import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps

@Parcelize
data class GutenbergPropsBuilder @JvmOverloads constructor(
    // parameters that are available at the time of construction
    private val enableMentions: Boolean,
    private val enableUnsupportedBlockEditor: Boolean,
    private val localeSlug: String,
    private val postType: String,
    private val editorTheme: Bundle?,

    // parameters that are not available at the time of construction and require setters
    private var translations: Bundle? = null,
    private var isDarkMode: Boolean? = null,
    private var htmlModeEnabled: Boolean? = null
) : Parcelable {
    fun build() = GutenbergProps(
            enableMentions = enableMentions,
            enableUnsupportedBlockEditor = enableUnsupportedBlockEditor,
            localeSlug = localeSlug,
            postType = postType,
            editorTheme = editorTheme,
            translations = requireNotNull(translations),
            isDarkMode = requireNotNull(isDarkMode),
            htmlModeEnabled = requireNotNull(htmlModeEnabled)
    )

    fun setTranslations(translations: Bundle) {
        this.translations = translations
    }

    fun setDarkMode(isDarkMode: Boolean) {
        this.isDarkMode = isDarkMode
    }

    fun setHtmlModeEnabled(htmlModeEnabled: Boolean) {
        this.htmlModeEnabled = htmlModeEnabled
    }
}
