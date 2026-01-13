package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem
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
        QUICK_LINKS("quick_links"),
        STATS("stats"),
        POST("post"),
        BLOGGING_PROMPT("blogging_prompt"),
        BLOGANUARY_NUDGE("bloganuary_nudge"),
        PROMOTE_WITH_BLAZE("promote_with_blaze"),
        BLAZE_CAMPAIGNS("blaze_campaigns"),
        PAGES("pages"),
        ACTIVITY("activity_log"),
        DASHBOARD_CARD_PLANS("dashboard_card_plans"),
        PERSONALIZE_CARD("personalize"),
        NO_CARDS("no_cards")
    }

    enum class StatsSubtype(val label: String) {
        TODAYS_STATS("todays_stats"),
        TODAYS_STATS_NUDGE("todays_stats_nudge")
    }

    enum class PostSubtype(val label: String) {
        DRAFT("draft"),
        SCHEDULED("scheduled")
    }

    enum class ActivityLogSubtype(val label: String) {
        ACTIVITY_LOG("activity_log")
    }

    enum class PagesSubType(val label: String) {
        CREATE_PAGE("create_page"),
        DRAFT("draft"),
        SCHEDULED("scheduled"),
        PUBLISHED("published")
    }

    enum class BlazeSubtype(val label: String) {
        NO_CAMPAIGNS("no_campaigns"),
        CAMPAIGNS("campaigns")
    }

    fun trackCardFooterLinkClicked(type: String, subtype: String) {
        analyticsTrackerWrapper.track(
            Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
            mapOf(
                TYPE to type,
                SUBTYPE to subtype
            )
        )
    }

    fun trackCardItemClicked(type: String, subtype: String) {
        val props = mapOf(TYPE to type, SUBTYPE to subtype)
        analyticsTrackerWrapper.track(Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED, props)
    }

    fun trackCardMoreMenuItemClicked(card: String, item: String) {
        analyticsTrackerWrapper.track(
            Stat.MY_SITE_DASHBOARD_CARD_MENU_ITEM_TAPPED,
            mapOf(
                CARD to card,
                ITEM to item
            )
        )
    }

    fun trackCardMoreMenuClicked(type: String) {
        analyticsTrackerWrapper.track(Stat.MY_SITE_DASHBOARD_CONTEXTUAL_MENU_ACCESSED, mapOf(CARD to type))
    }

    fun resetShown() {
        cardsShownTracker.reset()
    }

    fun trackShown(dashboardCards: List<MySiteCardAndItem.Card>) {
        cardsShownTracker.track(dashboardCards)
    }

    companion object {
        const val TYPE = "type"
        const val SUBTYPE = "subtype"
        const val STATS = "stats"
        const val ITEM = "item"
        const val CARD = "card"
    }
}

@Suppress("ComplexMethod")
fun MySiteCardAndItem.Type.toTypeValue(): Type {
    return when (this) {
        MySiteCardAndItem.Type.ERROR_CARD -> Type.ERROR
        MySiteCardAndItem.Type.QUICK_LINK_RIBBON -> Type.QUICK_LINKS
        MySiteCardAndItem.Type.TODAYS_STATS_CARD_ERROR -> Type.ERROR
        MySiteCardAndItem.Type.TODAYS_STATS_CARD -> Type.STATS
        MySiteCardAndItem.Type.POST_CARD_ERROR -> Type.ERROR
        MySiteCardAndItem.Type.POST_CARD_WITH_POST_ITEMS -> Type.POST
        MySiteCardAndItem.Type.BLOGANUARY_NUDGE_CARD -> Type.BLOGANUARY_NUDGE
        MySiteCardAndItem.Type.BLOGGING_PROMPT_CARD -> Type.BLOGGING_PROMPT
        MySiteCardAndItem.Type.PROMOTE_WITH_BLAZE_CARD -> Type.PROMOTE_WITH_BLAZE
        MySiteCardAndItem.Type.BLAZE_CAMPAIGNS_CARD -> Type.BLAZE_CAMPAIGNS
        MySiteCardAndItem.Type.DASHBOARD_PLANS_CARD -> Type.DASHBOARD_CARD_PLANS
        MySiteCardAndItem.Type.PAGES_CARD -> Type.PAGES
        MySiteCardAndItem.Type.PAGES_CARD_ERROR -> Type.ERROR
        MySiteCardAndItem.Type.ACTIVITY_CARD -> Type.ACTIVITY
        else -> {
            Type.ERROR
        }
    }
}

fun PostCardType.toSubtypeValue(): PostSubtype {
    return when (this) {
        PostCardType.DRAFT -> PostSubtype.DRAFT
        PostCardType.SCHEDULED -> PostSubtype.SCHEDULED
    }
}
