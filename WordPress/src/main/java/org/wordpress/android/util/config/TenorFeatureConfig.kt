package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

/**
 * Configuration of the Tenor gif selection. This is an example of a local only feature.
 */
class TenorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE
)
