package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    enum class Type(val label: String) {
        POST("post")
    }

    enum class PostSubtype(val label: String) {
        CREATE_FIRST("create_first"),
        CREATE_NEXT("create_next"),
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    fun trackPostCardFooterLinkClicked(postCardType: PostCardType) {
        trackCardFooterLinkClicked(Type.POST, postCardType.toSubtypeValue())
    }

    private fun trackCardFooterLinkClicked(
        type: Type,
        subtype: PostSubtype
    ) {
        analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(
                        TYPE to type.label,
                        SUBTYPE to subtype.label
                )
        )
    }

    private fun PostCardType.toSubtypeValue(): PostSubtype {
        return when (this) {
            PostCardType.CREATE_FIRST -> PostSubtype.CREATE_FIRST
            PostCardType.CREATE_NEXT -> PostSubtype.CREATE_NEXT
            PostCardType.DRAFT -> PostSubtype.DRAFT
            PostCardType.SCHEDULED -> PostSubtype.SCHEDULED
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val SUBTYPE = "subtype"
    }
}
