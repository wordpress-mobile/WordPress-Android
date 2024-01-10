package org.wordpress.android.ui.mysite.cards.dynamiccard

import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_CTA_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_HIDE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_TAPPED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private const val CARD_PROPERTY_ID = "id"
private const val CARD_PROPERTY_URL = "url"

class DynamicCardsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    private var cardShownTracked = mutableListOf<String>()

    fun resetShown() {
        cardShownTracked = mutableListOf()
    }

    fun trackShown(id: String) {
        if (!cardShownTracked.contains(id)) {
            cardShownTracked.add(id)
            analyticsTracker.track(
                stat = DYNAMIC_DASHBOARD_CARD_SHOWN,
                properties = mapOf(CARD_PROPERTY_ID to id)
            )
        }
    }

    fun trackCardTapped(id: String, url: String) {
        analyticsTracker.track(
            stat = DYNAMIC_DASHBOARD_CARD_TAPPED,
            properties = mapOf(CARD_PROPERTY_ID to id, CARD_PROPERTY_URL to url)
        )
    }

    fun trackCtaTapped(id: String, url: String) {
        analyticsTracker.track(
            stat = DYNAMIC_DASHBOARD_CARD_CTA_TAPPED,
            properties = mapOf(CARD_PROPERTY_ID to id, CARD_PROPERTY_URL to url)
        )
    }

    fun trackHideTapped(id: String) {
        analyticsTracker.track(
            stat = DYNAMIC_DASHBOARD_CARD_HIDE_TAPPED,
            properties = mapOf(CARD_PROPERTY_ID to id)
        )
    }
}
