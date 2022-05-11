package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloggingPromptsOnboardingAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackScreenShown() = analyticsTracker.track(
            Stat.BLOGGING_PROMPTS_INTRODUCTION_SCREEN_VIEWED, emptyMap()
    )

    fun trackTryItNowClicked() = analyticsTracker.track(
            Stat.BLOGGING_PROMPTS_INTRODUCTION_TRY_IT_NOW_CLICKED, emptyMap()
    )

    fun trackRemindMeClicked() = analyticsTracker.track(
            Stat.BLOGGING_PROMPTS_INTRODUCTION_REMIND_ME_CLICKED, emptyMap()
    )

    fun trackGotItClicked() = analyticsTracker.track(
            Stat.BLOGGING_PROMPTS_INTRODUCTION_GOT_IT_CLICKED, emptyMap()
    )
}
