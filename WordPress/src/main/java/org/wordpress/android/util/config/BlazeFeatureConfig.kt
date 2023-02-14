package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val BLAZE_FEATURE_REMOTE_FIELD = "blaze"

@Feature(BLAZE_FEATURE_REMOTE_FIELD, false)
class BlazeFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_BLAZE_FEATURE,
    BLAZE_FEATURE_REMOTE_FIELD
)
