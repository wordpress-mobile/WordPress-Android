package org.wordpress.android.util

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import org.wordpress.android.util.config.AppConfig
import org.wordpress.android.util.config.FeatureConfig
import javax.inject.Inject

/**
 * Configuration of the Scan feature.
 */
@FeatureInDevelopment
class ScanFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.SCAN_AVAILABLE
)
