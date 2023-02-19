package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class JetpackInstallFullPluginShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<Pair<MySiteCardAndItem.Type, Map<String, String>>>()

    fun resetShown() {
        cardsShownTracked.clear()
    }

    fun trackShown(itemType: MySiteCardAndItem.Type, tabType: MySiteTabType) {
        if (itemType == MySiteCardAndItem.Type.JETPACK_INSTALL_FULL_PLUGIN_CARD) {
            val props = mapOf(TAB_SOURCE to tabType.trackingLabel)

            val cardsShownTrackedPair = Pair(itemType, props)
            if (!cardsShownTracked.contains(cardsShownTrackedPair)) {
                cardsShownTracked.add(cardsShownTrackedPair)
                analyticsTrackerWrapper.track(
                    AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_VIEWED,
                    props
                )
            }
        }
    }

    companion object {
        private const val TAB_SOURCE = "tab_source"
    }
}
