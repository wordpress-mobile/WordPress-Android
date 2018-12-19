package org.wordpress.android.ui.sitecreation

import org.wordpress.android.analytics.AnalyticsTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewSiteCreationTracker @Inject constructor() {
    fun trackSiteCreated() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE)
    }
}
