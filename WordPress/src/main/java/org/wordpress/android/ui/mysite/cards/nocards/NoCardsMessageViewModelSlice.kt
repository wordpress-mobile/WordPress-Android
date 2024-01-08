package org.wordpress.android.ui.mysite.cards.nocards

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.DashboardPersonalizationFeatureConfig
import javax.inject.Inject

class NoCardsMessageViewModelSlice @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dashboardPersonalizationFeatureConfig: DashboardPersonalizationFeatureConfig
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    @Suppress("ReturnCount")
    fun buildNoCardsMessage(cardsResult: List<MySiteCardAndItem>): MySiteCardAndItem.Card.NoCardsMessage? {
        if (!dashboardPersonalizationFeatureConfig.isEnabled()) return null

        if (cardsResult.isEmpty()) return buildNoCardsMessage()

        val cards = cardsResult.filter {
            // if there is any card of these types then we don't show the no cards message
            it.type in listOfDashboardCardShownType()
        }

        if (cards.isEmpty())
            return buildNoCardsMessage()

        return null
    }

    private fun buildNoCardsMessage(): MySiteCardAndItem.Card.NoCardsMessage {
        return MySiteCardAndItem.Card.NoCardsMessage(
            title = UiString.UiStringRes(R.string.my_site_dashboard_no_cards_message_title),
            message = UiString.UiStringRes(R.string.my_site_dashboard_no_cards_message_description),
        )
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == MySiteCardAndItem.Type.NO_CARDS_MESSAGE) {
            if (!cardsShownTracked.contains(itemType)) {
                cardsShownTracked.add(itemType)
                analyticsTrackerWrapper.track(
                    AnalyticsTracker.Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                    mapOf(
                        CardsTracker.TYPE to CardsTracker.Type.NO_CARDS.label,
                    )
                )
            }
        }
    }

    fun resetShown() {
        cardsShownTracked.clear()
    }

    private fun listOfDashboardCardShownType(): List<MySiteCardAndItem.Type> {
        return listOf(
            MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD,
            MySiteCardAndItem.Type.ERROR_CARD,
            MySiteCardAndItem.Type.TODAYS_STATS_CARD_ERROR,
            MySiteCardAndItem.Type.TODAYS_STATS_CARD,
            MySiteCardAndItem.Type.POST_CARD_ERROR,
            MySiteCardAndItem.Type.POST_CARD_WITH_POST_ITEMS,
            MySiteCardAndItem.Type.BLOGGING_PROMPT_CARD,
            MySiteCardAndItem.Type.PROMOTE_WITH_BLAZE_CARD,
            MySiteCardAndItem.Type.BLAZE_CAMPAIGNS_CARD,
            MySiteCardAndItem.Type.DASHBOARD_PLANS_CARD,
            MySiteCardAndItem.Type.PAGES_CARD_ERROR,
            MySiteCardAndItem.Type.PAGES_CARD,
            MySiteCardAndItem.Type.ACTIVITY_CARD,
        )
    }
}
