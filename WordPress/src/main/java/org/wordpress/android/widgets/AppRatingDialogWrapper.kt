package org.wordpress.android.widgets

import org.wordpress.android.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * Mockable wrapper created for testing purposes.
 */
class AppRatingDialogWrapper @Inject constructor() {
    fun incrementInteractions(tracker: AnalyticsTracker.Stat) = AppRatingDialog.incrementInteractions(tracker)
}
