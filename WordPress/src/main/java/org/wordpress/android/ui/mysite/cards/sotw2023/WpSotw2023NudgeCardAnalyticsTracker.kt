package org.wordpress.android.ui.mysite.cards.sotw2023

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class WpSotw2023NudgeCardAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    private var cardShownTracked: Boolean = false

    fun resetShown() {
        cardShownTracked = false
    }

    fun trackShown() {
        if (!cardShownTracked) {
            cardShownTracked = true
            analyticsTracker.track(AnalyticsTracker.Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_SHOWN)
        }
    }

    fun trackCtaTapped() {
        analyticsTracker.track(AnalyticsTracker.Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_CTA_TAPPED)
    }

    fun trackHideTapped() {
        analyticsTracker.track(AnalyticsTracker.Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_HIDE_TAPPED)
    }
}
