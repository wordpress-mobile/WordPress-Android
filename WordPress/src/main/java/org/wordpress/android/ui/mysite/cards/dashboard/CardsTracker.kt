package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type.POST
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
    enum class Type(val label: String) {
        POST("post")
    }

    enum class Subtype(val label: String) {
        CREATE_FIRST("create_first"),
        CREATE_NEXT("create_next"),
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    fun trackDashboardPostCardFooterLinkClicked(postCardType: PostCardType) {
        trackDashboardCardFooterLinkClicked(POST, postCardType.toSubtypeValue())
    }

    private fun trackDashboardCardFooterLinkClicked(
        type: Type,
        subtype: Subtype
    ) {
        analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(
                        TYPE to type.label,
                        SUBTYPE to subtype.label
                )
        )
    }

    private fun PostCardType.toSubtypeValue(): Subtype {
        return when (this) {
            CREATE_FIRST -> Subtype.CREATE_FIRST
            CREATE_NEXT -> Subtype.CREATE_NEXT
            DRAFT -> Subtype.DRAFT
            SCHEDULED -> Subtype.SCHEDULED
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val SUBTYPE = "subtype"
    }
}
