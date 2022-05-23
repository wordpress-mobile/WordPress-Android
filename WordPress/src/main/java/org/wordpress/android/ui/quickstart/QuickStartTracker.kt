package org.wordpress.android.ui.quickstart

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_CUSTOMIZE_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QUICK_START_TYPE_GROW_VIEWED
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class QuickStartTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    @JvmOverloads
    fun track(stat: Stat, properties: Map<String, Any?>? = null) {
        val props = HashMap<String, Any?>()
        properties?.let { props.putAll(it) }
        props[SITE_TYPE] = appPrefsWrapper.getLastSelectedQuickStartType().trackingLabel
        analyticsTrackerWrapper.track(stat, props)
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

    companion object {
        private const val SITE_TYPE = "site_type"
    }
}
