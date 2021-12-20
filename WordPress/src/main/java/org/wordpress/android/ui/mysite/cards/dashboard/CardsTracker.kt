package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.DashboardCardPropertyType.POST
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.CREATE_FIRST
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.CREATE_NEXT
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.SCHEDULED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    enum class DashboardCardPropertyType(val label: String) {
        POST("post")
    }

    enum class DashboardCardPropertySubtype(val label: String) {
        CREATE_FIRST("create_first"),
        CREATE_NEXT("create_next"),
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    fun trackDashboardPostCardFooterLinkClicked(postCardType: PostCardType) {
        trackDashboardCardFooterLinkClicked(POST, postCardType.toSubtypeValue())
    }

    private fun trackDashboardCardFooterLinkClicked(
        type: DashboardCardPropertyType,
        subtype: DashboardCardPropertySubtype
    ) {
        analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(
                        TYPE to type.label,
                        SUBTYPE to subtype.label
                )
        )
    }

    private fun PostCardType.toSubtypeValue(): DashboardCardPropertySubtype {
        return when (this) {
            CREATE_FIRST -> DashboardCardPropertySubtype.CREATE_FIRST
            CREATE_NEXT -> DashboardCardPropertySubtype.CREATE_NEXT
            DRAFT -> DashboardCardPropertySubtype.DRAFT
            SCHEDULED -> DashboardCardPropertySubtype.SCHEDULED
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val SUBTYPE = "subtype"
    }
}
