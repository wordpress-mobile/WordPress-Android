package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteDefaultTabExperimentFeatureConfig.Companion.MY_SITE_DEFAULT_TAB_EXPERIMENT
import javax.inject.Inject

/**
 * Configuration of the 'My Site - Default Tab Experiment' - will guard the experiment
 */
@Feature(
        remoteField = MY_SITE_DEFAULT_TAB_EXPERIMENT,
        defaultValue = false
)
class MySiteDefaultTabExperimentFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DEFAULT_TAB_EXPERIMENT,
        MY_SITE_DEFAULT_TAB_EXPERIMENT
) {
    companion object {
        const val MY_SITE_DEFAULT_TAB_EXPERIMENT = "my_site_default_tab_experiment"
    }
}
