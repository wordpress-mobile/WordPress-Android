package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ScanScreenFeatureConfig.Companion.SCAN_SCREEN
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Scan Screen' feature.
 */
@Feature(remoteField = SCAN_SCREEN, defaultValue = true)
class ScanScreenFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.SCAN_SCREEN_AVAILABLE,
        SCAN_SCREEN
) {
    companion object {
        const val SCAN_SCREEN = "scan_screen"
    }
}
