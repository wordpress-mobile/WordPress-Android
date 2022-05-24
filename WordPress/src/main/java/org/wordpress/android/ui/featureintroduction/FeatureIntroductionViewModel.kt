package org.wordpress.android.ui.featureintroduction

import androidx.lifecycle.ViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class FeatureIntroductionViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    private var dismissAnalyticsEvent: Pair<Stat, Map<String, Any?>>? = null

    fun setDismissAnalyticsEvent(stat: Stat, properties: Map<String, Any?>) {
        dismissAnalyticsEvent = Pair(stat, properties)
    }

    fun onCloseButtonClick() {
        trackDismissAnalyticsEvent()
    }

    fun onBackButtonClick() {
        trackDismissAnalyticsEvent()
    }

    private fun trackDismissAnalyticsEvent() {
        dismissAnalyticsEvent?.let { (stat, properties) ->
            analyticsTracker.track(stat, properties)
        }
    }
}
