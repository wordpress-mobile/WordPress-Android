package org.wordpress.android.editor

import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps

@Parcelize
data class GutenbergPropsBuilder @JvmOverloads constructor(
    val enableMentions: Boolean,
    val enableUnsupportedBlockEditor: Boolean,
    val localeSlug: String,
    val postType: String,
    val editorTheme: Bundle?,
    var translations: Bundle? = null,
    var isDarkMode: Boolean? = null,
    var htmlModeEnabled: Boolean? = null
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
}
