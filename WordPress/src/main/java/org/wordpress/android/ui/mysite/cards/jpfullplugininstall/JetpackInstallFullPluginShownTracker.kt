package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class JetpackInstallFullPluginShownTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    fun resetShown() {
        cardsShownTracked.clear()
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == MySiteCardAndItem.Type.JETPACK_INSTALL_FULL_PLUGIN_CARD) {
            if (!cardsShownTracked.contains(itemType)) {
                cardsShownTracked.add(itemType)
                analyticsTrackerWrapper.track(
                    AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_VIEWED,
                )
            }
        }
    }

    companion object {
        private const val TAB_SOURCE = "tab_source"
    }
}
