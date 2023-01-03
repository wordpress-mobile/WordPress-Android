package org.wordpress.android.ui.mysite.cards.jetpackfeature

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.JETPACK_FEATURE_CARD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class JetpackFeatureCardShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    fun resetShown() {
        cardsShownTracked.clear()
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == JETPACK_FEATURE_CARD) {
            if (!cardsShownTracked.contains(itemType)) {
                cardsShownTracked.add(itemType)
                analyticsTrackerWrapper.track(
                        Stat.REMOVE_FEATURE_CARD_DISPLAYED,
                        mapOf("phase" to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName))
            }
        }
    }
}
