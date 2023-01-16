package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.StatsSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<Pair<String, String>>()

    fun reset() {
        cardsShownTracked.clear()
    }

    fun track(dashboardCards: DashboardCards) {
        dashboardCards.cards.takeIf { it.isNotEmpty() }?.forEach {
            trackCardShown(it)
        }
    }

    private fun trackCardShown(card: DashboardCard) = when (card) {
        is ErrorCard -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                Type.ERROR.label
            )
        )
        is TodaysStatsCard.Error -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                StatsSubtype.TODAYS_STATS.label
            )
        )
        is TodaysStatsCardWithData -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                StatsSubtype.TODAYS_STATS.label
            )
        )
        is PostCard.Error -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                Type.POST.label
            )
        )
        is PostCardWithoutPostItems -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                card.postCardType.toSubtypeValue().label
            )
        )
        is PostCardWithPostItems -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                card.postCardType.toSubtypeValue().label
            )
        )
        is BloggingPromptCardWithData -> trackCardShown(
            Pair(
                card.dashboardCardType.toTypeValue().label,
                Type.BLOGGING_PROMPT.label
            )
        )
    }

    fun trackQuickStartCardShown(quickStartType: QuickStartType) {
        trackCardShown(
            Pair(
                DashboardCardType.QUICK_START_CARD.toTypeValue().label,
                "quick_start_${quickStartType.trackingLabel}"
            )
        )
    }

    private fun trackCardShown(pair: Pair<String, String>) {
        if (!cardsShownTracked.contains(pair)) {
            cardsShownTracked.add(pair)
            analyticsTrackerWrapper.track(
                Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                mapOf(
                    CardsTracker.TYPE to pair.first,
                    CardsTracker.SUBTYPE to pair.second
                )
            )
        }
    }
}
