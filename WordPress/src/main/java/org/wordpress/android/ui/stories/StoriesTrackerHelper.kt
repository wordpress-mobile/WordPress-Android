package org.wordpress.android.ui.stories

import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject

class StoriesTrackerHelper @Inject constructor() {
    fun trackStorySaveResultEvent(event: StorySaveResult) {
        val properties = getCommonProperties(event)
        val stat = if (event.isSuccess()) Stat.STORY_SAVE_SUCCESSFUL else Stat.STORY_SAVE_ERROR
        var siteModel: SiteModel? = null
        event.metadata?.let {
            siteModel = it.getSerializable(WordPress.SITE) as SiteModel
        }

        siteModel?.let {
            AnalyticsUtils.trackWithSiteDetails(stat, it, properties)
        } ?: AnalyticsTracker.track(stat, properties)
    }

    private fun getCommonProperties(event: StorySaveResult): HashMap<String, Any> {
        val properties: HashMap<String, Any> = HashMap()
        properties.put("is_retry", event.isRetry)
        return properties
    }
}
