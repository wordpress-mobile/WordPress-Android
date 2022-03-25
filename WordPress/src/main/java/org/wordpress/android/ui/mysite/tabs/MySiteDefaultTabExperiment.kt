package org.wordpress.android.ui.mysite.tabs

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentVariationDashboardFeatureConfig
import javax.inject.Inject

class MySiteDefaultTabExperiment @Inject constructor(
    private val mySiteDefaultTabExperimentFeatureConfig: MySiteDefaultTabExperimentFeatureConfig,
    private val mySiteDefaultTabExperimentVariationDashboardFeatureConfig:
    MySiteDefaultTabExperimentVariationDashboardFeatureConfig,
    private val mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun checkAndSetVariantIfNeeded() {
        if (isExperimentRunning()) {
            if (isVariantNotAssigned()) {
                when (mySiteDefaultTabExperimentVariationDashboardFeatureConfig.isDashboardVariant()) {
                    true -> setExperimentVariant(MySiteTabExperimentVariant.DASHBOARD)
                    false -> setExperimentVariant(MySiteTabExperimentVariant.SITE_MENU)
                }
            }
        }
    }

    private fun isExperimentRunning() =
            mySiteDashboardTabsFeatureConfig.isEnabled() && mySiteDefaultTabExperimentFeatureConfig.isEnabled()

    private fun isVariantNotAssigned() =
            appPrefsWrapper.getMySiteDefaultTabExperimentVariant() == MySiteTabExperimentVariant.NONEXISTENT.label

    private fun setExperimentVariant(variant: MySiteTabExperimentVariant) {
        appPrefsWrapper.setMySiteDefaultTabExperimentVariant(variant.label)
    }

    fun getExperimentVariantForTracking() =
            mapOf(DEFAULT_TAB_EXPERIMENT to appPrefsWrapper.getMySiteDefaultTabExperimentVariant())

    companion object {
        private const val DEFAULT_TAB_EXPERIMENT = "default_tab_experiment"
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
