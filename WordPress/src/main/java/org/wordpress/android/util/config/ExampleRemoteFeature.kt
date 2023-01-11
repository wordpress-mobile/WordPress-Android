package org.wordpress.android.util.config

import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ExampleRemoteFeature.Companion.EXAMPLE_REMOTE_FEATURE_FIELD
import javax.inject.Inject

/**
 * Configuration of an example remote feature
 */
@Suppress("Unused")
@Feature(remoteField = EXAMPLE_REMOTE_FEATURE_FIELD, defaultValue = false)
class ExampleRemoteFeature
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    false,
    EXAMPLE_REMOTE_FEATURE_FIELD
) {
    companion object {
        const val EXAMPLE_REMOTE_FEATURE_FIELD = "example_remote_feature_field"
    }
}
