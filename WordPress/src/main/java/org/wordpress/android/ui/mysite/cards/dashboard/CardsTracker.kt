package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.PostSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsTracker @Inject constructor(
    private val cardsShownTracker: CardsShownTracker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    enum class Type(val label: String) {
        ERROR("error"),
        TODAYS_STATS("todays_stats"),
        POST("post")
    }

    enum class PostSubtype(val label: String) {
        CREATE_FIRST("create_first"),
        CREATE_NEXT("create_next"),
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    fun trackPostCardFooterLinkClicked(postCardType: PostCardType) {
        trackCardFooterLinkClicked(Type.POST.label, postCardType.toSubtypeValue().label)
    }

    fun trackPostItemClicked(postCardType: PostCardType) {
        trackCardPostItemClicked(Type.POST.label, postCardType.toSubtypeValue().label)
    }

    private fun trackCardFooterLinkClicked(type: String, subtype: String) {
        analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(
                        TYPE to type,
                        SUBTYPE to subtype
                )
        )
    }

    private fun trackCardPostItemClicked(type: String, subtype: String) {
        analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED,
                mapOf(
                        TYPE to type,
                        SUBTYPE to subtype
                )
        )
    }

    fun resetShown() {
        cardsShownTracker.reset()
    }

    fun trackShown(dashboardCards: DashboardCards) {
        cardsShownTracker.track(dashboardCards)
    }

    companion object {
        const val TYPE = "type"
        const val SUBTYPE = "subtype"
    }
}

fun DashboardCardType.toTypeValue(): Type {
    return when (this) {
        DashboardCardType.ERROR_CARD -> Type.ERROR
        DashboardCardType.TODAYS_STATS_CARD_ERROR -> Type.ERROR
        DashboardCardType.TODAYS_STATS_CARD -> Type.TODAYS_STATS
        DashboardCardType.POST_CARD_ERROR -> Type.ERROR
        DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS -> Type.POST
        DashboardCardType.POST_CARD_WITH_POST_ITEMS -> Type.POST
    }
}

fun PostCardType.toSubtypeValue(): PostSubtype {
    return when (this) {
        PostCardType.CREATE_FIRST -> PostSubtype.CREATE_FIRST
        PostCardType.CREATE_NEXT -> PostSubtype.CREATE_NEXT
        PostCardType.DRAFT -> PostSubtype.DRAFT
        PostCardType.SCHEDULED -> PostSubtype.SCHEDULED
    }
}
