package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Scan Screen' feature.
 */
@FeatureInDevelopment
class ScanScreenFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.SCAN_SCREEN_AVAILABLE
)
