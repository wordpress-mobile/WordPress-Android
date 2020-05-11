package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Experiment
import javax.inject.Inject

/**
 * Configuration of the Tenor gif selection.
 */
@Experiment(remoteField = "pokus", defaultVariant = "control")
class TenorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE
)
