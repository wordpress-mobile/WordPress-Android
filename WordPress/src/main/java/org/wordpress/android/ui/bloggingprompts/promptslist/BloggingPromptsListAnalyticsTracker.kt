package org.wordpress.android.ui.bloggingprompts.promptslist

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloggingPromptsListAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackScreenShown() = analyticsTracker.track(
            Stat.BLOGGING_PROMPTS_LIST_SCREEN_VIEWED, emptyMap()
    )
}
