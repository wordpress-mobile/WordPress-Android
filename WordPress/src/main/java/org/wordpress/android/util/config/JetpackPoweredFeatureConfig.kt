package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration for Jetpack Powered indicators
 */
@Feature(JetpackPoweredFeatureConfig.JETPACK_POWERED_REMOTE_FIELD, true)
class JetpackPoweredFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_POWERED,
    JETPACK_POWERED_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_POWERED_REMOTE_FIELD = "jetpack_powered_remote_field"
    }
}
