package org.wordpress.android.ui.quickstart

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class QuickStartTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val quickStartRepository: QuickStartRepository
) {
    @JvmOverloads
    fun track(stat: Stat, properties: Map<String, Any?>? = null) {
        val props = HashMap<String, Any?>()
        properties?.let { props.putAll(it) }
        props[SITE_TYPE] = quickStartRepository.quickStartType.label
        analyticsTrackerWrapper.track(stat, props)
    }

    companion object {
        private const val SITE_TYPE = "site_type"
    }
}
