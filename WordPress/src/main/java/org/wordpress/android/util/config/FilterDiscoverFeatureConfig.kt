package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.FilterDiscoverFeatureConfig.Companion.FILTER_DISCOVER
import javax.inject.Inject

/**
 * Configuration of the filter for discover or topic feature.
 */
@Feature(remoteField = FILTER_DISCOVER, defaultValue = true)
class FilterDiscoverFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.ENABLE_FILTER_FOR_DISCOVER,
        FILTER_DISCOVER
) {
    companion object {
        const val FILTER_DISCOVER = "filter_discover"
    }
}
