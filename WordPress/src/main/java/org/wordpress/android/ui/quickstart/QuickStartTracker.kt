package org.wordpress.android.ui.quickstart

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class QuickStartTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private val cardsShownTracked = mutableListOf<Pair<MySiteCardAndItem.Type, Map<String, String>>>()

    @JvmOverloads
    fun track(stat: Stat, properties: Map<String, Any?>? = null) {
        val props = HashMap<String, Any?>()
        properties?.let { props.putAll(it) }
        props[SITE_TYPE] = appPrefsWrapper.getLastSelectedQuickStartType().trackingLabel
        analyticsTrackerWrapper.track(stat, props)
    }

    fun trackShown(itemType: MySiteCardAndItem.Type, tabType: MySiteTabType) {
        if (itemType == QUICK_START_CARD) {
            val props = mapOf(
                    TAB to tabType.trackingLabel,
                    SITE_TYPE to appPrefsWrapper.getLastSelectedQuickStartType().trackingLabel
            )
            val cardsShownTrackedPair = Pair(itemType, props)
            if (!cardsShownTracked.contains(cardsShownTrackedPair)) {
                cardsShownTracked.add(cardsShownTrackedPair)
                analyticsTrackerWrapper.track(Stat.QUICK_START_CARD_SHOWN, props)
            }
        }
    }

    fun resetShown() {
        cardsShownTracked.clear()
    }

    companion object {
        private const val SITE_TYPE = "site_type"
        private const val TAB = "tab"
    }
}
