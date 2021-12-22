package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class CardsShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    fun resetShown() {
        cardsShownTracked.clear()
    }

    fun trackCardShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == DOMAIN_REGISTRATION_CARD) {
            if (!cardsShownTracked.contains(itemType)) {
                cardsShownTracked.add(itemType)
                analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN)
            }
        }
    }
}
