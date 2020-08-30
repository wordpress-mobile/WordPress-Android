package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

/**
 * Configuration of the Consolidated media picker
 */
class ConsolidatedMediaPickerFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.CONSOLIDATED_MEDIA_PICKER
)
