package org.wordpress.android.ui.mysite.items

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class SiteItemsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    enum class Type(val label: String) {
        POSTS("posts"),
        STATS("stats")
    }

    fun trackSiteItemClicked(listItemAction: ListItemAction) = trackSiteItemClicked(listItemAction.toTypeValue())

    private fun trackSiteItemClicked(type: Type?) {
        type?.let {
            analyticsTrackerWrapper.track(Stat.MY_SITE_MENU_ITEM_TAPPED, mapOf(TYPE to it.label))
        }
    }

    private fun ListItemAction.toTypeValue(): Type? {
        return when (this) {
            ListItemAction.POSTS -> Type.POSTS
            ListItemAction.STATS -> Type.STATS
            else -> null
        }
    }

    companion object {
        private const val TYPE = "type"
    }
}
