package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<Pair<DashboardCardType, PostCardType>>()
    private var hasErrorCardShownTracked = false

    enum class Type(val label: String) {
        ERROR("error"),
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

    fun trackCardsShown(dashboardCards: DashboardCards) {
        dashboardCards.cards.takeIf { it.isNotEmpty() }?.forEach {
            trackCardShown(it)
        }
    }

    fun resetShown() {
        cardsShownTracked.clear()
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

    private fun trackCardShown(card: DashboardCard) = when (card) {
        is ErrorCard -> trackErrorCardShown()
        is PostCardWithPostItems -> trackPostCardShown(Pair(card.dashboardCardType, card.postCardType))
        is PostCardWithoutPostItems -> trackPostCardShown(Pair(card.dashboardCardType, card.postCardType))
    }

    private fun trackPostCardShown(pair: Pair<DashboardCardType, PostCardType>) {
        if (!cardsShownTracked.contains(pair)) {
            cardsShownTracked.add(pair)
            analyticsTrackerWrapper.track(
                    Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                    mapOf(TYPE to Type.POST.label, SUBTYPE to pair.second.toSubtypeValue().label)
            )
        }
    }

    private fun trackErrorCardShown() {
        if (!hasErrorCardShownTracked) {
            hasErrorCardShownTracked = true
            analyticsTrackerWrapper.track(Stat.MY_SITE_DASHBOARD_CARD_SHOWN, mapOf(TYPE to Type.ERROR.label))
        }
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
