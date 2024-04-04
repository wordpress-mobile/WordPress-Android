package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val SYNC_PUBLISHING_FEATURE_REMOTE_FIELD = "sync_publishing"

@Feature(SYNC_PUBLISHING_FEATURE_REMOTE_FIELD, false)
class SyncPublishingFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.SYNC_PUBLISHING,
    SYNC_PUBLISHING_FEATURE_REMOTE_FIELD
)
