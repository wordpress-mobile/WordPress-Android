package org.wordpress.android.ui.main.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.voicetocontent.VoiceToContentFeatureUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.ReaderFloatingButtonFeatureConfig
import javax.inject.Inject

class MainCreateSheetHelper @Inject constructor(
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val readerFloatingButtonFeatureConfig: ReaderFloatingButtonFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val buildConfig: BuildConfigWrapper,
    private val siteUtils: SiteUtilsWrapper,
) {
    fun shouldShowFabForPage(page: PageType?): Boolean {
        val enabledForPage = page == PageType.MY_SITE ||
                (page == PageType.READER && readerFloatingButtonFeatureConfig.isEnabled())
        return buildConfig.isCreateFabEnabled && enabledForPage
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun canCreatePost(): Boolean = true // for completeness

    fun canCreatePage(site: SiteModel?, page: PageType?): Boolean {
        return siteUtils.hasFullAccessToContent(site) && page == PageType.MY_SITE
    }

    fun canCreatePostFromAudio(site: SiteModel?): Boolean {
        return voiceToContentFeatureUtils.isVoiceToContentEnabled() && siteUtils.hasFullAccessToContent(site)
    }

    suspend fun canCreatePromptAnswer(): Boolean = bloggingPromptsSettingsHelper.shouldShowPromptsFeature()
}
