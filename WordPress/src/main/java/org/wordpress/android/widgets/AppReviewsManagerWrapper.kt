package org.wordpress.android.widgets

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.Note
import javax.inject.Inject

/**
 * Mockable wrapper created for testing purposes.
 */
class AppReviewsManagerWrapper @Inject constructor() {
    fun onNotificationReceived(note: Note) = AppReviewManager.onNotificationReceived(note)
    fun incrementInteractions(tracker: AnalyticsTracker.Stat) = AppReviewManager.incrementInteractions(tracker)
}
