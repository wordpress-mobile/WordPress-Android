package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val WP_INDIVIDUAL_PLUGIN_OVERLAY_REMOTE_FIELD = "wp_individual_plugin_overlay"

@Feature(WP_INDIVIDUAL_PLUGIN_OVERLAY_REMOTE_FIELD, true)
class WPIndividualPluginOverlayFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.WP_INDIVIDUAL_PLUGIN_OVERLAY,
    WP_INDIVIDUAL_PLUGIN_OVERLAY_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && !BuildConfig.IS_JETPACK_APP
    }
}
