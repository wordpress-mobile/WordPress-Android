package org.wordpress.android.ui.mysite.tabs

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentVariationDashboardFeatureConfig
import javax.inject.Inject

class MySiteDefaultTabExperiment @Inject constructor(
    private val mySiteDefaultTabExperimentFeatureConfig: MySiteDefaultTabExperimentFeatureConfig,
    private val mySiteDefaultTabExperimentVariationDashboardFeatureConfig:
    MySiteDefaultTabExperimentVariationDashboardFeatureConfig,
    private val mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun checkAndSetVariantIfNeeded() {
        if (isExperimentRunning()) {
            if (!isVariantAssigned()) {
                setVariantAssigned()
                when (mySiteDefaultTabExperimentVariationDashboardFeatureConfig.isDashboardVariant()) {
                    true -> setExperimentVariant(VARIANT_HOME)
                    false -> setExperimentVariant(VARIANT_SITE_MENU)
                }
                analyticsTrackerWrapper.setInjectExperimentProperties(getVariantMapForTracking())
                analyticsTrackerWrapper.track(Stat.MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_ASSIGNED)
            }
        }
    }

    fun checkAndSetTrackingPropertiesIfNeeded() {
        if (isExperimentRunning()) {
            analyticsTrackerWrapper.setInjectExperimentProperties(getVariantMapForTracking())
        }
    }

    private fun isExperimentRunning() =
            mySiteDashboardTabsFeatureConfig.isEnabled() && mySiteDefaultTabExperimentFeatureConfig.isEnabled()

    private fun isVariantAssigned() = appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()

    private fun setVariantAssigned() = appPrefsWrapper.setMySiteDefaultTabExperimentVariantAssigned()

    private fun setExperimentVariant(variant: String) =
            appPrefsWrapper.setInitialScreenFromMySiteDefaultTabExperimentVariant(variant)

    private fun getVariantMapForTracking() =
            mapOf(DEFAULT_TAB_EXPERIMENT to appPrefsWrapper.getMySiteDefaultTabExperimentVariant())

    companion object {
        private const val DEFAULT_TAB_EXPERIMENT = "default_tab_experiment"
        private const val VARIANT_DASHBOARD = "dashboard"
        private const val VARIANT_SITE_MENU = "site_menu"
        private const val VARIANT_HOME = "home"
        private const val NONEXISTENT = "nonexistent"
    }
}
enum class MySiteTabExperimentVariant(val label: String) {
    NONEXISTENT(MySiteTabExperimentVariant.VARIANT_NONEXISTENT),
    DASHBOARD(MySiteTabExperimentVariant.VARIANT_DASHBOARD),
    SITE_MENU(MySiteTabExperimentVariant.VARIANT_SITE_MENU);

    override fun toString() = label

    companion object {
        private const val VARIANT_NONEXISTENT = "nonexistent"
        private const val VARIANT_DASHBOARD = "dashboard"
        private const val VARIANT_SITE_MENU = "site_menu"

        @JvmStatic
        fun fromString(label: String) = when {
            NONEXISTENT.label == label -> NONEXISTENT
            DASHBOARD.label == label -> DASHBOARD
            SITE_MENU.label == label -> SITE_MENU
            else -> NONEXISTENT
        }
    }
}
