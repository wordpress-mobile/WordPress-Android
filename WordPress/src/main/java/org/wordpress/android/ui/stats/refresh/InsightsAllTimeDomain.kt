package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.stats.StatsEvents
import javax.inject.Inject

class InsightsAllTimeDomain
@Inject constructor(val dispatcher: Dispatcher) {
    fun onEventMainThread(event: StatsEvents.InsightsAllTimeUpdated) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return
        }

        mInsightsAllTimeModel = event.mInsightsAllTimeModel
        updateUI()
    }

    fun onEventMainThread(event: StatsEvents.SectionUpdateError) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return
        }

        mInsightsAllTimeModel = null
        showErrorUI(event.mError)
    }
}
