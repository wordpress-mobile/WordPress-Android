package org.wordpress.android.editor.gutenberg

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps

@Parcelize
@SuppressLint("ParcelCreator")
data class GutenbergPropsBuilder(
    private val enableContactInfoBlock: Boolean,
    private val enableLayoutGridBlock: Boolean,
    private val enableFacebookEmbed: Boolean,
    private val enableInstagramEmbed: Boolean,
    private val enableLoomEmbed: Boolean,
    private val enableSmartframeEmbed: Boolean,
    private val enableMediaFilesCollectionBlocks: Boolean,
    private val enableMentions: Boolean,
    private val enableXPosts: Boolean,
    private val enableUnsupportedBlockEditor: Boolean,
    private val unsupportedBlockEditorSwitch: Boolean,
    private val isAudioBlockMediaUploadEnabled: Boolean,
    private val enableReusableBlock: Boolean,
    private val localeSlug: String,
    private val postType: String,
    private val featuredImageId: Int,
    private val editorTheme: Bundle?,
    private val enableEditorOnboarding: Boolean,
    private val firstGutenbergEditorSession: Boolean
) : Parcelable {
    fun build(activity: Activity, isHtmlModeEnabled: Boolean) = GutenbergProps(
            enableContactInfoBlock = enableContactInfoBlock,
            enableLayoutGridBlock = enableLayoutGridBlock,
            enableFacebookEmbed = enableFacebookEmbed,
            enableInstagramEmbed = enableInstagramEmbed,
            enableLoomEmbed = enableLoomEmbed,
            enableSmartframeEmbed = enableSmartframeEmbed,
            enableMediaFilesCollectionBlocks = enableMediaFilesCollectionBlocks,
            enableMentions = enableMentions,
            enableXPosts = enableXPosts,
            enableUnsupportedBlockEditor = enableUnsupportedBlockEditor,
            canEnableUnsupportedBlockEditor = unsupportedBlockEditorSwitch,
            isAudioBlockMediaUploadEnabled = isAudioBlockMediaUploadEnabled,
            enableReusableBlock = enableReusableBlock,
            localeSlug = localeSlug,
            postType = postType,
            featuredImageId = featuredImageId,
            editorTheme = editorTheme,
            translations = GutenbergUtils.getTranslations(activity),
            isDarkMode = GutenbergUtils.isDarkMode(activity),
            htmlModeEnabled = isHtmlModeEnabled,
            enableEditorOnboarding = enableEditorOnboarding,
            firstGutenbergEditorSession = firstGutenbergEditorSession
    )
}
