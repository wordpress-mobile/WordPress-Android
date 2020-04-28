package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

/**
 * An example implementation of a feature configuration.
 */
@SuppressWarnings("Unused")
class TenorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE,
        "tenor_available"
)
