package org.wordpress.android.ui.posts

import android.content.Context
import org.wordpress.android.editor.gutenberg.GutenbergPropsBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPMediaUtils
import java.util.Locale

// Until EditPostActivity gets converted to Kotlin, this file is for those parts of EditPostActivity
// that would really benefit from being written in Kotlin now.
internal object EditPostActivityHelper {
    fun getGutenbergPropsBuilder(
        site: SiteModel,
        isPage: Boolean,
        isXPostsCapable: Boolean?,
        isJetpackSsoEnabled: Boolean,
        editPostRepository: EditPostRepository,
        editorThemeStore: EditorThemeStore,
        context: Context
    ): GutenbergPropsBuilder {
        val isFreeWPCom = site.isWPCom && SiteUtils.onFreePlan(site)

        val languageString = LocaleManager.getLanguage(context)
        val localeSlug = languageString.replace("_", "-").lowercase(Locale.ENGLISH)

        // If isXPostsCapable has not been set, default to allowing xPosts
        val enableXPosts = isXPostsCapable ?: true

        return GutenbergPropsBuilder(
                enableContactInfoBlock = SiteUtils.supportsContactInfoFeature(site),
                enableLayoutGridBlock = SiteUtils.supportsLayoutGridFeature(site),
                enableTiledGalleryBlock = SiteUtils.supportsTiledGalleryFeature(site),
                enableFacebookEmbed = SiteUtils.supportsEmbedVariationFeature(
                        site,
                        SiteUtils.WP_FACEBOOK_EMBED_JETPACK_VERSION
                ),
                enableInstagramEmbed = SiteUtils.supportsEmbedVariationFeature(
                        site,
                        SiteUtils.WP_INSTAGRAM_EMBED_JETPACK_VERSION
                ),
                enableLoomEmbed = SiteUtils.supportsEmbedVariationFeature(
                        site,
                        SiteUtils.WP_SMARTFRAME_EMBED_JETPACK_VERSION
                ),
                enableSmartframeEmbed = SiteUtils.supportsEmbedVariationFeature(
                        site,
                        SiteUtils.WP_SMARTFRAME_EMBED_JETPACK_VERSION
                ),
                enableMediaFilesCollectionBlocks = SiteUtils.supportsStoriesFeature(site),
                enableMentions = site.isUsingWpComRestApi,
                enableXPosts = enableXPosts,
                enableUnsupportedBlockEditor = site.isWPCom || isJetpackSsoEnabled,
                unsupportedBlockEditorSwitch = site.isJetpackConnected && !isJetpackSsoEnabled,
                canUploadMedia = WPMediaUtils.currentUserCanUploadMedia(site),
                isAudioBlockMediaUploadEnabled = !isFreeWPCom,
                enableReusableBlock = site.isWPCom || site.isWPComAtomic,
                localeSlug = localeSlug,
                postType = if (isPage) "page" else "post",
                featuredImageId = editPostRepository.featuredImageId.toInt(),
                editorTheme = editorThemeStore.getEditorThemeForSite(site)?.themeSupport?.toBundle()
        )
    }
}
