package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteDefaultTabExperimentVariationDashboardFeatureConfig.Companion.MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_DASHBOARD
import javax.inject.Inject

/**
 * Configuration of the 'My Site - Default tab Experiment' -
 * Identifies if this is variant Dashboard with a isEnabled=true, and indicates site_menu isEnabled=false
 */
@Feature(
        remoteField = MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_DASHBOARD,
        defaultValue = false
)
class MySiteDefaultTabExperimentVariationDashboardFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_DASHBOARD,
        MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_DASHBOARD
) {
    companion object {
        const val MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_DASHBOARD = "my_site_default_tab_experiment_variant_dashboard"
    }
}
