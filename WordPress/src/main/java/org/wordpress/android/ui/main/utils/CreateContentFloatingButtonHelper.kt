package org.wordpress.android.ui.main.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.voicetocontent.VoiceToContentFeatureUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.ReaderFloatingButtonFeatureConfig
import javax.inject.Inject

class CreateContentFloatingButtonHelper @Inject constructor(
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val readerFloatingButtonFeatureConfig: ReaderFloatingButtonFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val buildConfigWrapper: BuildConfigWrapper,
) {
    fun shouldShowFabForPage(page: PageType?): Boolean {
        val enabledForPage = page == PageType.MY_SITE ||
                (page == PageType.READER && readerFloatingButtonFeatureConfig.isEnabled())
        return buildConfigWrapper.isCreateFabEnabled && enabledForPage
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun canCreatePost(): Boolean = true // for completeness

    fun canCreatePage(site: SiteModel?, page: PageType?): Boolean {
        return SiteUtils.hasFullAccessToContent(site) && page == PageType.MY_SITE
    }

    fun canCreatePostFromAudio(site: SiteModel?): Boolean {
        return voiceToContentFeatureUtils.isVoiceToContentEnabled() && SiteUtils.hasFullAccessToContent(site)
    }

    suspend fun canCreatePromptAnswer(): Boolean = bloggingPromptsSettingsHelper.shouldShowPromptsFeature()
}
