package org.wordpress.android.editor.gutenberg

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps

@Parcelize
data class GutenbergPropsBuilder(
    private val enableMediaFilesCollectionBlocks: Boolean,
    private val enableMentions: Boolean,
    private val enableUnsupportedBlockEditor: Boolean,
    private val unsupportedBlockEditorSwitch: Boolean,
    private val enablePreviewMode: Boolean,
    private val localeSlug: String,
    private val postType: String,
    private val editorTheme: Bundle?
) : Parcelable {
    fun build(activity: Activity, isHtmlModeEnabled: Boolean) = GutenbergProps(
            enableMediaFilesCollectionBlocks = enableMediaFilesCollectionBlocks,
            enableMentions = enableMentions,
            enableUnsupportedBlockEditor = enableUnsupportedBlockEditor,
            canEnableUnsupportedBlockEditor = unsupportedBlockEditorSwitch,
            localeSlug = localeSlug,
            postType = postType,
            editorTheme = editorTheme,
            translations = GutenbergUtils.getTranslations(activity),
            isDarkMode = GutenbergUtils.isDarkMode(activity),
            htmlModeEnabled = isHtmlModeEnabled,
            isPreview = enablePreviewMode,
    )
}
