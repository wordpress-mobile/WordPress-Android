package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val IN_APP_UPDATES_FEATURE_REMOTE_FIELD = "in_app_updates"

@Feature(IN_APP_UPDATES_FEATURE_REMOTE_FIELD, false)
class InAppUpdatesFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_IN_APP_UPDATES,
    IN_APP_UPDATES_FEATURE_REMOTE_FIELD
)
