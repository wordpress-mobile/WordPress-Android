package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class VoiceToContentLogger @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appLogWrapper: AppLogWrapper
) {
    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(stat)
    }

    fun track(stat: Stat, properties: Map<String, Any?>) {
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun logError(message: String) {
        appLogWrapper.e(POSTS, "Voice to content $message")
    }
}
