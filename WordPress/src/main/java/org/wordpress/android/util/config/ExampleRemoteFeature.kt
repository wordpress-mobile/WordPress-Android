package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ExampleRemoteFeature.Companion.EXAMPLE_REMOTE_FEATURE_FIELD
import javax.inject.Inject

/**
 * Configuration of an example remote feature
 */
@SuppressWarnings("Unused")
@Feature(remoteField = EXAMPLE_REMOTE_FEATURE_FIELD, defaultValue = false)
class ExampleRemoteFeature
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE
) {
    companion object {
        const val EXAMPLE_REMOTE_FEATURE_FIELD = "example_remote_feature_field"
    }
}
