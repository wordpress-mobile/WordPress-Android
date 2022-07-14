package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration for Jetpack Powered indicators
 *
 * TODO: When it is ready to be rolled out uncomment the lines 12 and 19, remove line 13 and this to-do
 */
//@Feature(JetpackPoweredFeatureConfig.JETPACK_POWERED_REMOTE_FIELD, true)
@FeatureInDevelopment
class JetpackPoweredFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.JETPACK_POWERED,
//        JETPACK_POWERED_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_POWERED_REMOTE_FIELD = "jetpack_powered_remote_field"
    }
}
