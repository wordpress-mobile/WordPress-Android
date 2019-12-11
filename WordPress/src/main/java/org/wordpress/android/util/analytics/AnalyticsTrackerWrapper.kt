package org.wordpress.android.util.analytics

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: Stat) {
        AnalyticsTracker.track(stat)
    }

    fun track(stat: Stat, properties: Map<String, *>) {
        AnalyticsTracker.track(stat, properties)
    }

    fun track(stat: Stat, site: SiteModel) {
        AnalyticsUtils.trackWithSiteDetails(stat, site)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
        AnalyticsTracker.track(stat, errorContext, errorType, errorDescription)
    }
}
