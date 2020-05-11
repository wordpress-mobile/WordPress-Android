package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Experiment
import org.wordpress.android.util.config.ExampleExperimentConfig.Companion
import javax.inject.Inject

/**
 * Configuration of the Tenor gif selection.
 */
@SuppressWarnings("Unused")
@Experiment(remoteField = ExampleExperimentConfig.REMOTE_FIELD, defaultVariant = ExampleExperimentConfig.CONTROL_GROUP)
class TenorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE,
        "tenor_available"
)
