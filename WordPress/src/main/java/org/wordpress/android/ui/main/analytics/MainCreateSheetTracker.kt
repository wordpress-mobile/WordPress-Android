package org.wordpress.android.ui.main.analytics

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.WPMainNavigationView
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Locale
import javax.inject.Inject

class MainCreateSheetTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    fun trackActionTapped(page: WPMainNavigationView.PageType, actionType: MainActionListItem.ActionType) {
        val stat = when (page) {
            WPMainNavigationView.PageType.MY_SITE -> AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_ACTION_TAPPED
            WPMainNavigationView.PageType.READER -> AnalyticsTracker.Stat.READER_CREATE_SHEET_ACTION_TAPPED
            else -> return
        }
        val properties = mapOf("action" to actionType.name.lowercase(Locale.ROOT))
        analyticsTracker.track(stat, properties)
    }

    fun trackAnswerPromptActionTapped(page: WPMainNavigationView.PageType, attribution: BloggingPromptAttribution) {
        val properties = mapOf("attribution" to attribution.value).filterValues { it.isNotBlank() }
        val stat = when (page) {
            WPMainNavigationView.PageType.MY_SITE -> AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_ANSWER_PROMPT_TAPPED
            WPMainNavigationView.PageType.READER -> AnalyticsTracker.Stat.READER_CREATE_SHEET_ANSWER_PROMPT_TAPPED
            else -> return
        }
        analyticsTracker.track(stat, properties)
    }

    fun trackHelpPromptActionTapped(page: WPMainNavigationView.PageType) {
        val stat = when (page) {
            WPMainNavigationView.PageType.MY_SITE -> AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_PROMPT_HELP_TAPPED
            WPMainNavigationView.PageType.READER -> AnalyticsTracker.Stat.READER_CREATE_SHEET_PROMPT_HELP_TAPPED
            else -> return
        }
        analyticsTracker.track(stat)
    }

    fun trackSheetShown(page: WPMainNavigationView.PageType) {
        val stat = when (page) {
            WPMainNavigationView.PageType.MY_SITE -> AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_SHOWN
            WPMainNavigationView.PageType.READER -> AnalyticsTracker.Stat.READER_CREATE_SHEET_SHOWN
            else -> return
        }
        analyticsTracker.track(stat)
    }

    fun trackFabShown(page: WPMainNavigationView.PageType) {
        val stat = when (page) {
            WPMainNavigationView.PageType.MY_SITE -> AnalyticsTracker.Stat.MY_SITE_CREATE_FAB_SHOWN
            WPMainNavigationView.PageType.READER -> AnalyticsTracker.Stat.READER_CREATE_FAB_SHOWN
            else -> return
        }
        analyticsTracker.track(stat)
    }

    fun trackCreateActionsSheetCard(actions: List<MainActionListItem>) {
        if (actions.any { it is MainActionListItem.AnswerBloggingPromptAction }) {
            analyticsTracker.track(AnalyticsTracker.Stat.BLOGGING_PROMPTS_CREATE_SHEET_CARD_VIEWED)
        }
    }
}
