package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_BUTTON_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_CANCELLED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_COMPLETED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_START
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_RECEIVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_SCHEDULED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_SCREEN_SHOWN
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Button.CONTINUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.SiteType.SELF_HOSTED
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.SiteType.WORDPRESS_COM
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloggingRemindersAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val siteStore: SiteStore
) {
    private var siteType: SiteType? = null

    fun setSite(siteId: Int) = siteStore.getSiteByLocalId(siteId)?.let { site ->
        siteType = if (site.isWPCom) WORDPRESS_COM else SELF_HOSTED
    }

    fun trackScreenShown(screen: Screen) = track(
            BLOGGING_REMINDERS_SCREEN_SHOWN,
            mapOf(SCREEN_KEY to screen.trackingName)
    )

    fun trackPrimaryButtonPressed(screen: Screen) = trackButtonPressed(screen, CONTINUE)

    private fun trackButtonPressed(screen: Screen, button: Button) = track(
            BLOGGING_REMINDERS_BUTTON_PRESSED,
            mapOf(
                    SCREEN_KEY to screen.trackingName,
                    BUTTON_KEY to button.trackingName
            )
    )

    fun trackFlowStart(source: Source) = track(
            BLOGGING_REMINDERS_FLOW_START,
            mapOf(SOURCE_KEY to source.trackingName)
    )

    fun trackFlowDismissed(source: Screen) = track(
            BLOGGING_REMINDERS_FLOW_DISMISSED,
            mapOf(SOURCE_KEY to source.trackingName)
    )

    fun trackFlowCompleted() = track(BLOGGING_REMINDERS_FLOW_COMPLETED)

    fun trackRemindersScheduled(daysCount: Int, timeSelected: CharSequence) = track(
            BLOGGING_REMINDERS_SCHEDULED,
            mapOf(DAYS_OF_WEEK_COUNT_KEY to daysCount, SELECTED_TIME_KEY to timeSelected)
    )

    fun trackRemindersCancelled() = track(BLOGGING_REMINDERS_CANCELLED)

    fun trackNotificationReceived(promptIncluded: Boolean) = track(
            BLOGGING_REMINDERS_NOTIFICATION_RECEIVED, mapOf(PROMPT_INCLUDED to "$promptIncluded")
    )

    fun trackRemindersIncludePromptPressed(promptEnabled: Boolean) =
            track(Stat.BLOGGING_REMINDERS_INCLUDE_PROMPT_TAPPED, mapOf(PROMPT_ENABLED_KEY to "$promptEnabled"))

    fun trackRemindersIncludePromptHelpPressed() = track(Stat.BLOGGING_REMINDERS_INCLUDE_PROMPT_HELP_TAPPED)

    private fun track(stat: Stat, properties: Map<String, Any?> = emptyMap()) = analyticsTracker.track(
            stat,
            properties + (BLOG_TYPE_KEY to siteType?.trackingName)
    )

    private enum class SiteType(val trackingName: String) {
        WORDPRESS_COM("wpcom"),
        SELF_HOSTED("self_hosted")
    }

    // We only track the primary button for now, as we don't have a dismiss button like WPiOS does.
    private enum class Button(val trackingName: String) {
        CONTINUE("continue")
    }

    enum class Source(val trackingName: String) {
        PUBLISH_FLOW("publish_flow"),
        BLOG_SETTINGS("blog_settings"),
        NOTIFICATION_SETTINGS("notification_settings"),
        BLOGGING_PROMPTS_ONBOARDING("blogging_prompts_onboarding"),
    }

    companion object {
        private const val BLOG_TYPE_KEY = "blog_type"
        private const val SCREEN_KEY = "screen"
        private const val BUTTON_KEY = "button"
        private const val SOURCE_KEY = "source"
        private const val PROMPT_ENABLED_KEY = "enabled"
        private const val DAYS_OF_WEEK_COUNT_KEY = "days_of_week_count"
        private const val SELECTED_TIME_KEY = "selected_time"
        private const val PROMPT_INCLUDED = "prompt_included"
    }
}
