package org.wordpress.android.ui.mysite.cards.personalize

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PersonalizeCardShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    fun resetShown() {
        cardsShownTracked.clear()
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == MySiteCardAndItem.Type.PERSONALIZE_CARD) {
            if (!cardsShownTracked.contains(itemType)) {
                cardsShownTracked.add(itemType)
                analyticsTrackerWrapper.track(
                    AnalyticsTracker.Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                    mapOf(
                        CardsTracker.TYPE to CardsTracker.Type.PERSONALIZE_CARD.label,
                        CardsTracker.SUBTYPE to CardsTracker.Type.PERSONALIZE_CARD.label
                    )
                )
            }
        }
    }
}
