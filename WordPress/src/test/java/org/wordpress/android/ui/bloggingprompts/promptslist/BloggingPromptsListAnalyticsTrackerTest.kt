package org.wordpress.android.ui.bloggingprompts.promptslist

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class BloggingPromptsListAnalyticsTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val tracker = BloggingPromptsListAnalyticsTracker(analyticsTracker)

    @Test
    fun `Should track screen viewed`() {
        tracker.trackScreenShown()
        verify(analyticsTracker).track(
            Stat.BLOGGING_PROMPTS_LIST_SCREEN_VIEWED,
            emptyMap()
        )
    }
}
