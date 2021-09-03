package org.wordpress.android.ui.stories

import com.wordpress.stories.compose.StoriesAnalyticsListener
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat

/**
 * Receives tracker-agnostic analytics events from the Stories library and forwards them to [AnalyticsTracker].
 */
class StoriesAnalyticsReceiver : StoriesAnalyticsListener {
    override fun trackStoryTextChanged(properties: Map<String, *>) {
        AnalyticsTracker.track(Stat.STORY_TEXT_CHANGED, properties)
    }
}
