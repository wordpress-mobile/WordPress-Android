package org.wordpress.android.ui.main.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.voicetocontent.VoiceToContentFeatureUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.ReaderFloatingButtonFeatureConfig
import java.util.Locale
import javax.inject.Inject

class MainCreateSheetHelper @Inject constructor(
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val readerFloatingButtonFeatureConfig: ReaderFloatingButtonFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val buildConfig: BuildConfigWrapper,
    private val siteUtils: SiteUtilsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
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

    // region Analytics
    fun trackActionTapped(page: PageType, actionType: MainActionListItem.ActionType) {
        val stat = when (page) {
            PageType.MY_SITE -> Stat.MY_SITE_CREATE_SHEET_ACTION_TAPPED
            PageType.READER -> Stat.READER_CREATE_SHEET_ACTION_TAPPED
            else -> return
        }
        val properties = mapOf("action" to actionType.name.lowercase(Locale.ROOT))
        analyticsTracker.track(stat, properties)
    }

    fun trackAnswerPromptActionTapped(page: PageType, attribution: BloggingPromptAttribution) {
        val properties = mapOf("attribution" to attribution.value).filterValues { it.isNotBlank() }
        val stat = when (page) {
            PageType.MY_SITE -> Stat.MY_SITE_CREATE_SHEET_ANSWER_PROMPT_TAPPED
            PageType.READER -> Stat.READER_CREATE_SHEET_ANSWER_PROMPT_TAPPED
            else -> return
        }
        analyticsTracker.track(stat, properties)
    }

    fun trackHelpPromptActionTapped(page: PageType) {
        val stat = when (page) {
            PageType.MY_SITE -> Stat.MY_SITE_CREATE_SHEET_PROMPT_HELP_TAPPED
            PageType.READER -> Stat.READER_CREATE_SHEET_PROMPT_HELP_TAPPED
            else -> return
        }
        analyticsTracker.track(stat)
    }

    fun trackSheetShown(page: PageType) {
        val stat = when (page) {
            PageType.MY_SITE -> Stat.MY_SITE_CREATE_SHEET_SHOWN
            PageType.READER -> Stat.READER_CREATE_SHEET_SHOWN
            else -> return
        }
        analyticsTracker.track(stat)
    }

    fun trackFabShown(page: PageType) {
        val stat = when (page) {
            PageType.MY_SITE -> Stat.MY_SITE_CREATE_FAB_SHOWN
            PageType.READER -> Stat.READER_CREATE_FAB_SHOWN
            else -> return
        }
        analyticsTracker.track(stat)
    }
    // endregion
}
