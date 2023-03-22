package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class WPIndividualPluginOverlayFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.WP_INDIVIDUAL_PLUGIN_OVERLAY,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && !BuildConfig.IS_JETPACK_APP
    }
}
