package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.HashMap
import javax.inject.Inject

class StatsTracker
@Inject constructor(private val analyticsTrackerWrapper: AnalyticsTrackerWrapper){
    fun trackGranular(stat: Stat, statsGranularity: StatsGranularity) {
        val props = HashMap<String, String>()
        props["granularity"] = statsGranularity.toString()
        analyticsTrackerWrapper.track(stat, props)
    }
}
