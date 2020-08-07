package org.wordpress.android.editor.gutenberg

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps

@Parcelize
data class GutenbergPropsBuilder(
    private val enableMentions: Boolean,
    private val enableUnsupportedBlockEditor: Boolean,
    private val enableModalLayoutPicker: Boolean,
    private val localeSlug: String,
    private val postType: String,
    private val editorTheme: Bundle?
) : Parcelable {
    fun build(activity: Activity, isHtmlModeEnabled: Boolean) = GutenbergProps(
            enableMentions = enableMentions,
            enableUnsupportedBlockEditor = enableUnsupportedBlockEditor,
            localeSlug = localeSlug,
            postType = postType,
            editorTheme = editorTheme,
            translations = GutenbergUtils.getTranslations(activity),
            isDarkMode = GutenbergUtils.isDarkMode(activity),
            htmlModeEnabled = isHtmlModeEnabled,
            isModalLayoutPickerEnabled = enableModalLayoutPicker
    )
}
