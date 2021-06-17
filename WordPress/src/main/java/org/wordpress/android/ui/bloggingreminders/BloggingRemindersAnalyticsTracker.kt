package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.SiteType.SELF_HOSTED
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.SiteType.WORDPRESS_COM
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloggingRemindersAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val siteStore: SiteStore
) {
    private var siteType: SiteType? = null

    fun setSite(siteId: Int) = siteStore.getSiteByLocalId(siteId)?.let { site ->
        siteType = if (site.isWPCom) WORDPRESS_COM else SELF_HOSTED
    }

    private fun track(stat: Stat, properties: Map<String, Any?> = emptyMap()) = analyticsTracker.track(
            stat,
            properties + (BLOG_TYPE_KEY to siteType?.trackingName)
    )

    private enum class SiteType(val trackingName: String) {
        WORDPRESS_COM("wpcom"),
        SELF_HOSTED("self_hosted")
    }

    companion object {
        private const val BLOG_TYPE_KEY = "blog_type"
    }
}
