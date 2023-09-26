package org.wordpress.android.ui.quickstart

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_VIEWED
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class QuickStartTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val cardsShownTracked = mutableListOf<Pair<MySiteCardAndItem.Type, Map<String, String>>>()

    @JvmOverloads
    fun track(stat: Stat, properties: Map<String, Any?>? = null) {
        val props = HashMap<String, Any?>()
        properties?.let { props.putAll(it) }
        props[SITE_TYPE] = getLastSelectedQuickStartType().trackingLabel
        analyticsTrackerWrapper.track(stat, props)
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        if (itemType == QUICK_START_CARD) {
            val props = mapOf(
                SITE_TYPE to getLastSelectedQuickStartType().trackingLabel
            )
            val cardsShownTrackedPair = Pair(itemType, props)
            if (!cardsShownTracked.contains(cardsShownTrackedPair)) {
                cardsShownTracked.add(cardsShownTrackedPair)
                analyticsTrackerWrapper.track(Stat.QUICK_START_CARD_SHOWN, props)
            }
        }
    }

    fun trackQuickStartListViewed(tasksType: QuickStartTaskType) {
        when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> track(QUICK_START_TYPE_CUSTOMIZE_VIEWED)
            QuickStartTaskType.GROW -> track(QUICK_START_TYPE_GROW_VIEWED)
            QuickStartTaskType.GET_TO_KNOW_APP -> track(QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED)
            QuickStartTaskType.UNKNOWN -> Unit // Do Nothing
        }
    }

    fun trackQuickStartListDismissed(tasksType: QuickStartTaskType) {
        when (tasksType) {
            QuickStartTaskType.CUSTOMIZE -> track(QUICK_START_TYPE_CUSTOMIZE_DISMISSED)
            QuickStartTaskType.GROW -> track(QUICK_START_TYPE_GROW_DISMISSED)
            QuickStartTaskType.GET_TO_KNOW_APP -> track(QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED)
            QuickStartTaskType.UNKNOWN -> Unit // Do Nothing
        }
    }

    fun trackMoreMenuClicked(card: QuickStartCardType) {
        analyticsTrackerWrapper.track(
            Stat.MY_SITE_DASHBOARD_CONTEXTUAL_MENU_ACCESSED,
            mapOf(CardsTracker.CARD to card.toQuickStartMenuCard().label)
        )
    }

    fun trackMoreMenuItemClicked(card: QuickStartCardType) {
        analyticsTrackerWrapper.track(
            Stat.MY_SITE_DASHBOARD_CARD_MENU_ITEM_TAPPED,
            mapOf(
                CARD to card.toQuickStartMenuCard().label,
                ITEM to QuickStartMenuItemType.HIDE_THIS.label
            )
        )
    }
    fun resetShown() {
        cardsShownTracked.clear()
    }

    private fun getLastSelectedQuickStartType(): QuickStartType {
        return selectedSiteRepository.getSelectedSite()?.let {
            val siteLocalId = it.id.toLong()
            appPrefsWrapper.getLastSelectedQuickStartTypeForSite(siteLocalId)
        } ?: NewSiteQuickStartType
    }

    enum class QuickStartMenuItemType(val label: String) {
        HIDE_THIS("hide_this")
    }

    enum class QuickStartMenuCard(val label: String) {
        GET_TO_KNOW_THE_APP("get_to_know_the_app"),
        NEXT_STEPS("next_steps")
    }

    private fun QuickStartCardType.toQuickStartMenuCard(): QuickStartMenuCard {
        return when (this) {
            QuickStartCardType.GET_TO_KNOW_THE_APP -> QuickStartMenuCard.GET_TO_KNOW_THE_APP
            QuickStartCardType.NEXT_STEPS -> QuickStartMenuCard.NEXT_STEPS
        }
    }
    companion object {
        private const val SITE_TYPE = "site_type"
        private const val TAB = "tab"
        private const val CARD = "card"
        private const val ITEM = "item"
    }
}
