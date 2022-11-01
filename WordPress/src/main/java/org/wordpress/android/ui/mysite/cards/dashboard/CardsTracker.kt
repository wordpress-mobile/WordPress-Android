package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.PostSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.QuickStartSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsTracker @Inject constructor(
    private val cardsShownTracker: CardsShownTracker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val quickStartTracker: QuickStartTracker
) {
    enum class Type(val label: String) {
        ERROR("error"),
        QUICK_START("quick_start"),
        STATS("stats"),
        POST("post"),
        BLOGGING_PROMPT("blogging_prompt")
    }

    enum class QuickStartSubtype(val label: String) {
        CUSTOMIZE("customize"),
        GROW("grow"),
        GET_TO_KNOW_APP("get_to_know_app"),
        UNKNOWN("unkown")
    }

    enum class StatsSubtype(val label: String) {
        TODAYS_STATS("todays_stats"),
        TODAYS_STATS_NUDGE("todays_stats_nudge")
    }

    enum class PostSubtype(val label: String) {
        CREATE_FIRST("create_first"),
        CREATE_NEXT("create_next"),
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    fun trackQuickStartCardItemClicked(quickStartTaskType: QuickStartTaskType) {
        trackCardItemClicked(Type.QUICK_START.label, quickStartTaskType.toSubtypeValue().label)
    }

    fun trackTodaysStatsCardGetMoreViewsNudgeClicked() {
        trackCardItemClicked(Type.STATS.label, StatsSubtype.TODAYS_STATS_NUDGE.label)
    }

    fun trackTodaysStatsCardFooterLinkClicked() {
        trackCardFooterLinkClicked(Type.STATS.label, StatsSubtype.TODAYS_STATS.label)
    }

    fun trackTodaysStatsCardClicked() {
        trackCardItemClicked(Type.STATS.label, StatsSubtype.TODAYS_STATS.label)
    }

    fun trackPostCardFooterLinkClicked(postCardType: PostCardType) {
        trackCardFooterLinkClicked(Type.POST.label, postCardType.toSubtypeValue().label)
    }

    fun trackPostItemClicked(postCardType: PostCardType) {
        trackCardItemClicked(Type.POST.label, postCardType.toSubtypeValue().label)
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

    private fun trackCardItemClicked(type: String, subtype: String) {
        val props = mapOf(TYPE to type, SUBTYPE to subtype)
        if (type == Type.QUICK_START.label) {
            quickStartTracker.track(Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED, props)
        } else {
            analyticsTrackerWrapper.track(Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED, props)
        }
    }

    fun resetShown() {
        cardsShownTracker.reset()
    }

    fun trackShown(dashboardCards: DashboardCards) {
        cardsShownTracker.track(dashboardCards)
    }

    fun trackQuickStartCardShown(quickStartType: QuickStartType) {
        cardsShownTracker.trackQuickStartCardShown(quickStartType)
    }

    companion object {
        const val TYPE = "type"
        const val SUBTYPE = "subtype"
        const val STATS = "stats"
    }
}

fun DashboardCardType.toTypeValue(): Type {
    return when (this) {
        DashboardCardType.ERROR_CARD -> Type.ERROR
        DashboardCardType.QUICK_START_CARD -> Type.QUICK_START
        DashboardCardType.TODAYS_STATS_CARD_ERROR -> Type.ERROR
        DashboardCardType.TODAYS_STATS_CARD -> Type.STATS
        DashboardCardType.POST_CARD_ERROR -> Type.ERROR
        DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS -> Type.POST
        DashboardCardType.POST_CARD_WITH_POST_ITEMS -> Type.POST
        DashboardCardType.BLOGGING_PROMPT_CARD -> Type.BLOGGING_PROMPT
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

fun QuickStartTaskType.toSubtypeValue(): QuickStartSubtype {
    return when (this) {
        QuickStartTaskType.CUSTOMIZE -> QuickStartSubtype.CUSTOMIZE
        QuickStartTaskType.GROW -> QuickStartSubtype.GROW
        QuickStartTaskType.GET_TO_KNOW_APP -> QuickStartSubtype.GET_TO_KNOW_APP
        QuickStartTaskType.UNKNOWN -> QuickStartSubtype.UNKNOWN
    }
}
